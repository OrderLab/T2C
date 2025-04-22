count = 0
cpu = 0.0
mem = 0.0

path = "zookeeper.log"
f = open(path, "r")
for line in f:
    arr = line.split()
    cpu += float(arr[8])
    mem += float(arr[5][:-1])
    count += 1

print(f"CPU: {cpu/count}%, Memory: {mem/count}g, Sample: {count}")
