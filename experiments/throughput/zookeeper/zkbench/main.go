package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"time"

	zkb "zkbench/bench"
)

var (
	conf      = flag.String("conf", "bench.conf", "Benchmark configuration file")
	outprefix = flag.String("outprefix", "zkresult", "Benchmark stat filename prefix")
	nonstop   = flag.Bool("nonstop", false, "Run the benchmarks non-stop")
	purge     = flag.Bool("purge", false, "Purge all prior test data")
	rawstat   = flag.Bool("rawstat", false, "Log the raw benchmark stats")
)

type logWriter struct {
}

func (writer logWriter) Write(bytes []byte) (int, error) {
	return fmt.Print(time.Now().UTC().Format("2006-01-02T15:04:05.999Z") + string(bytes))
}

func main() {
	flag.Parse()
	config, err := zkb.ParseConfig(*conf)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Fail to parse config: %v\n", err)
		os.Exit(1)
	}
	fmt.Println(zkb.TypeStr(config.Type))

	log.SetFlags(0)
	log.SetOutput(new(logWriter))

	b := new(zkb.Benchmark)
	b.BenchConfig = *config
	b.Init()
	if *purge {
		fmt.Println("Start purging test data")
		b.Done()
		fmt.Println("Done")
		return
	}
	b.SmokeTest()
	current := time.Now()
	prefix := *outprefix + "-" + current.Format("2006-01-02-15_04_05") + "-"
	var iter int64 = 1
	for {
		b.Run(prefix, *rawstat, *nonstop, iter)
		if !*nonstop {
			break
		}
		time.Sleep(30000 * time.Millisecond)
		iter++
	}
	if b.Cleanup {
		b.Done()
	}
}
