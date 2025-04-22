package bench

import (
	"time"
)

type BenchLatency struct {
	Start   time.Time
	Latency time.Duration
}

type BenchStat struct {
	Ops          int64
	Errors       int64
	OpType       string
	StartTime    time.Time
	EndTime      time.Time
	Latencies    []BenchLatency
	MinLatency   time.Duration
	MaxLatency   time.Duration
	AvgLatency   time.Duration
	NinetyNinethLatency  int64
	TotalLatency time.Duration
	Throughput   float64
}

func (self *BenchStat) Merge(other *BenchStat) {
	self.Ops += other.Ops
	self.Errors += other.Errors
	// other starts earlier than me
	if self.StartTime.After(other.StartTime) {
		self.StartTime = other.StartTime
	}
	// other ends later than me
	if other.EndTime.After(self.EndTime) {
		self.EndTime = other.EndTime
	}
	// concatenate two slices
	self.Latencies = append(self.Latencies, other.Latencies...)
	if self.MinLatency > other.MinLatency {
		self.MinLatency = other.MinLatency
	}
	if self.MaxLatency < other.MaxLatency {
		self.MaxLatency = other.MaxLatency
	}
	self.TotalLatency += other.TotalLatency
	// recalculate average latency
	self.AvgLatency = self.TotalLatency / time.Duration(self.Ops)
	self.Throughput = float64(self.Ops) / self.TotalLatency.Seconds()
}
