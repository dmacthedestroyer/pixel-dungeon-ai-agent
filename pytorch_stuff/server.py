from analysis_shattered import state_to_junk
from flask import Flask, request
from model import DQN

import json
import torch

model = DQN()
model.load_state_dict(torch.load("first_model.pt"))
model.eval()

app = Flask(__name__)

@app.route("/")
def hello_world():
    return "<p>Hello, World!</p>"

@app.route('/test', methods=["POST"])
def test_post_with_body():
    body_stuff = request.get_json()
    print(json.dumps(body_stuff, indent=2))

    return ['a', 2, 'b', 23]


@app.route("/action-values", methods=["POST"])
def eval_state():
    state = request.get_json()
    junk = state_to_junk(state)
    tensor = torch.tensor([s or 0 for s in junk], dtype=torch.float)
    action_values = model(tensor)

    return action_values.tolist()

