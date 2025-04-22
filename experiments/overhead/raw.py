path_raw = "zookeeper_raw.log"
path = "zookeeper.log"
pid = "722833"

fin = open(path_raw, "r")
out = open(path, "w")
for line in fin:
    if line.strip().startswith(pid):
        out.write(line)

fin.close()
out.close()