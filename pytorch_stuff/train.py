import math
import os
import pickle
import random
from collections import namedtuple

import torch
import torch.nn as nn
import torch.optim as optim
from analysis_shattered import chunked, gen_transitions, state_to_junk
from model import DQN

device = torch.device(
    "cuda" if torch.cuda.is_available() else
    "mps" if torch.backends.mps.is_available() else
    "cpu"
)


print("device", device)


Transition = namedtuple('Transition',
                        ('state', 'action', 'next_state', 'reward'))


# BATCH_SIZE is the number of transitions sampled from the replay buffer
# GAMMA is the discount factor as mentioned in the previous section
# EPS_START is the starting value of epsilon
# EPS_END is the final value of epsilon
# EPS_DECAY controls the rate of exponential decay of epsilon, higher means a slower decay
# TAU is the update rate of the target network
# LR is the learning rate of the ``AdamW`` optimizer

BATCH_SIZE = 128
GAMMA = 0.99
EPS_START = 0.9
EPS_END = 0.01
EPS_DECAY = 2500
TAU = 0.005
LR = 3e-4


# Get number of actions from gym action space
n_actions = 9
# TODO populate with our data (from analysis file)
path = "./data"

print("building transitions...")
if not os.path.exists("./transitions.pickle"):
    print("pickling transitions")
    transitions = [Transition(
    torch.tensor([s or 0 for s in state], dtype=torch.float), 
    torch.tensor([action], dtype=torch.int64), 
    torch.tensor([s or 0 for s in next_state], dtype=torch.float), 
    torch.tensor([reward], dtype=torch.float)
    ) for state, action, next_state, reward in gen_transitions(path)]
    with open("./transitions.pickle", 'wb') as file:
        pickle.dump(transitions, file)
else:
    print("eating transition pickles")
    with open("./transitions.pickle", 'rb') as file:
        transitions = pickle.load(file)

print(len(transitions), " transitions")

# TODO make some replacement for env
# Get the number of state observations
# state, info = env.reset()
n_observations = len(transitions[0].state) # total number of observations that we have
print("n_observations", n_observations)

policy_net = DQN().to(device)
target_net = DQN().to(device)
target_net.load_state_dict(policy_net.state_dict())

optimizer = optim.AdamW(policy_net.parameters(), lr=LR, amsgrad=True)


steps_done = 0


def select_action(state):
    global steps_done
    sample = random.random()
    eps_threshold = EPS_END + (EPS_START - EPS_END) * \
        math.exp(-1. * steps_done / EPS_DECAY)
    steps_done += 1
    if sample > eps_threshold:
        with torch.no_grad():
            # t.max(1) will return the largest column value of each row.
            # second column on max result is index of where max element was
            # found, so we pick action with the larger expected reward.
            return policy_net(state).max(1).indices.view(1, 1)
    else:
        return torch.tensor([[random.randint(1, 9)]], device=device, dtype=torch.long)


def optimize_model(transition_batch: list[Transition]):
    state_batch = torch.stack([t.state for t in transition_batch])
    action_batch = torch.stack([t.action for t in transition_batch])
    next_state_batch= torch.stack([t.next_state for t in transition_batch])
    reward_batch = torch.cat([t.reward for t in transition_batch])

    # Compute Q(s_t, a) - the model computes Q(s_t), then we select the
    # columns of actions taken. These are the actions which would've been taken
    # for each batch state according to policy_net
    input_tensor = policy_net(state_batch)
    state_action_values = input_tensor.gather(1, action_batch)

    # Compute V(s_{t+1}) for all next states.
    # Expected values of actions for non_final_next_states are computed based
    # on the "older" target_net; selecting their best reward with max(1).values
    # This is merged based on the mask, such that we'll have either the expected
    # state value or 0 in case the state was final.
    with torch.no_grad():
        next_state_values, _ = target_net(next_state_batch).max(1)
    # Compute the expected Q values
    expected_state_action_values = (next_state_values * GAMMA) + reward_batch

    # Compute Huber loss
    criterion = nn.SmoothL1Loss()
    loss = criterion(state_action_values, expected_state_action_values.unsqueeze(1))

    # Optimize the model
    optimizer.zero_grad()
    loss.backward()
    # In-place gradient clipping
    torch.nn.utils.clip_grad_value_(policy_net.parameters(), 100)
    optimizer.step()

def soft_update_model(transition_batch: list[Transition]):
    """
    smoothing transition of target_net towards policy_net somehow ¯\_(ツ)_/¯
    """
    for _ in range(len(transition_batch)):
        target_net_state_dict = target_net.state_dict()
        policy_net_state_dict = policy_net.state_dict()
        for key in policy_net_state_dict:
            target_net_state_dict[key] = policy_net_state_dict[key]*TAU + target_net_state_dict[key]*(1-TAU)
        target_net.load_state_dict(target_net_state_dict)

def save_model(model_file: str):
    torch.save(policy_net.state_dict(), model_file)


def train_model_mass_data():
    print("training model...")
    for i, chunk in enumerate(chunked(transitions, BATCH_SIZE)):
        print(f"chunk {i}:")
        soft_update_model(chunk)
        print("\tsoft upate done")
        optimize_model(chunk)
        print("\toptimize done")
    print("saving model...")
    save_model("first_model.pt")

train_model_mass_data()
