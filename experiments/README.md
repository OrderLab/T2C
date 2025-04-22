# T2C-EvalAutomatons

## Installation
You need to install these software
1. [Golang](https://go.dev/doc/install). Used for zookeeper's throughput evaluation. Follow the link to install golang. 
Check whether your installation has succeed using `go version`

2. python2. Used for running YCSB for the throughput evaluation. Ubuntu 18.04 or 20.04 should have it by default. Check whether your installation has succeed using `python2 --version`

3. Nix package manager. Used to manage a different jdk version and leiningen version for false positive evaluation.
```
curl --proto '=https' --tlsv1.2 -sSf -L https://install.determinate.systems/nix | sh -s -- install
```
Check whether your installation has succeed using `nix --version`

4. Direnv. So that the nix environment is activated directly.
```
sudo apt install direnv
echo "eval \"\$(direnv hook bash)\"" >> ~/.bashrc
source ~/.bashrc
```
Allow the direnv config in jepsen folder by running
```
cd false_positive/jepsen
direnv allow
cd ..
```
Check whether the installation has succeed by `cd false_positive/jepsen`

## Run experiments
Follow the readme in each folder