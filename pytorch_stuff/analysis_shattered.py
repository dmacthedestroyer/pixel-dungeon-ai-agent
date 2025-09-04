import os
import json
from itertools import chain


path = "./data"
mob_ids = json.load(open("mob_names.json"))

def progression_score(state):
    max_depth = int(state["game_bundle"]["maxDepth"])
    hero_level = int(state["game_bundle"]["hero"]["lvl"])

    return min((max_depth*hero_level*65), 50000)

def gen_lines(data_path: str):
    for file in os.listdir(data_path): 
        if file.endswith(".jsonl"):
            for line in open(f"{data_path}/{file}"):
                yield json.loads(line)

# all_mobs = chain.from_iterable(line["state"]["level_bundle"]["level"]["mobs"] for line in gen_transitions(path))
# mob_names = {mob["__className"] for mob in all_mobs}

# for i, name in enumerate(mob_names):
#     print(name, i)

def state_to_junk(data: dict[str, any]):
    map = data["level_bundle"]["level"]["map"]
    level_width = data["level_bundle"]["level"]["width"]
    hero_pos = data["game_bundle"]["hero"]["pos"]
    mobs = data["level_bundle"]["level"]["mobs"]

    projected_coords = list(gen_indexes(hero_pos, level_width))

    projected_map = {x: map[x] if 0 <= x < len(map) else None for x in projected_coords}

    mob_data = {m["pos"]: [m["HP"], mob_ids.get(m["__className"])] for m in mobs if m["seen"] and m["pos"] in projected_coords}

    tensor_data = chain.from_iterable([[projected_map.get(x, None)] + mob_data.get(x, [None, None]) for x in projected_coords])
    return tensor_data


def gen_indexes(pos, width, proj_size=20):
    pos_x, pos_y = gen_coords(pos, width)
    for dx in range(-proj_size//2, proj_size//2):
        for dy in range(-proj_size//2, proj_size//2):
            yield ungen_coords(pos_x + dx, pos_y + dy, width)

def gen_coords(pos, width):
    x = pos % width
    y = pos // width
    return (x, y)

def ungen_coords(x, y, width):
    pos = x + y * width
    return pos

def gen_transitions(data_path):
    for line in gen_lines(data_path):
        yield (list(state_to_junk(line["state"])), line["action"], list(state_to_junk(line["next_state"])), progression_score(line["next_state"]))
        
def chunked(my_list, chunk_size):
    chunks = []
    for i in range(0, len(my_list), chunk_size):
        chunks.append(my_list[i:i + chunk_size])
    return chunks