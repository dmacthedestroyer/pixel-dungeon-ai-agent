import os
import json

files = [file for file in os.listdir(".") if file.endswith(".jsonl")]

# total_lines = sum(sum(1 for _ in open(file)) for file in files)

total_lines = 0
thing_counter = dict()

def progression_score(state):
    max_depth = int(state["game_bundle"]["maxDepth"])
    hero_level = int(state["game_bundle"]["hero"]["lvl"])

    return min((max_depth*hero_level*65), 50000)

for file in files:
    for line_no, line in enumerate(open(file)):
        total_lines += 1
        parsed_line = json.loads(line)

        thing = parsed_line["state"]["game_bundle"]["maxDepth"]
        if thing in thing_counter:
            thing_counter[thing] = thing_counter[thing] + 1
        else:
            thing_counter[thing] = 1
            # print("\n".join([f"{k}: {v}" for k, v in thing_counter.items()]))
            # print("\n")
        if total_lines % 1000 == 0:
            print(f"total lines: {total_lines}")
            print("\n".join([f"{k}\t{v}" for k, v in thing_counter.items()]))


        # level_thing = parsed_line['next_state']['level_bundle']['level']
        # level_height = level_thing["height"]
        # level_width = level_thing["width"]
        # level_size = len(level_thing['visited'])
        # level_sizes.add((level_width, level_height, level_size))
        # print(f"{file}:{line_no}: {level_width}x{level_height} ({level_size} cells) -- {len(level_sizes)} different sizes")

print(f"total lines: {total_lines}")
print("\n".join([f"{k}\t{v}" for k, v in thing_counter.items()]))