package config

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"
)

type Config struct {
	KVs  map[string]string
	File string
}

func ParseConfig(file string) (*Config, error) {
	fp, err := os.Open(file)
	if err != nil {
		return nil, err
	}
	scanner := bufio.NewScanner(fp)
	kvs := make(map[string]string)
	lineno := 0
	prefix := ""
	for scanner.Scan() {
		lineno += 1
		line := scanner.Text()
		line = strings.TrimSpace(line)
		idx := strings.Index(line, "#")
		if idx >= 0 {
			line = line[:idx]
		}
		if len(line) == 0 {
			continue
		}
		if line[0] == '[' && line[len(line)-1] == ']' {
			prefix = line[1 : len(line)-1]
			continue
		}
		parts := strings.Split(line, "=")
		if len(parts) != 2 {
			return nil, fmt.Errorf("Wrong format at line %d: must be [key] = [value]", lineno)
		}
		key := strings.TrimSpace(parts[0])
		val := strings.TrimSpace(parts[1])
		if len(key) == 0 || len(val) == 0 {
			return nil, fmt.Errorf("Empty key or value at line %d", lineno)
		}
		_, ok := kvs[key]
		if ok {
			return nil, fmt.Errorf("Key redefined at line %d", lineno)
		}
		if len(prefix) > 0 {
			key = prefix + "." + key
		}
		kvs[key] = val
	}
	return &Config{KVs: kvs, File: file}, nil
}

func (self *Config) GetKeys(prefix string) []string {
	var keys []string
	for key, _ := range self.KVs {
		if strings.HasPrefix(key, prefix) {
			keys = append(keys, key)
		}
	}
	return keys
}

func (self *Config) GetInt(key string) (int, error) {
	val, ok := self.KVs[key]
	if !ok {
		return -1, fmt.Errorf("Key %s does not exist", key)
	}
	return strconv.Atoi(val)
}

func (self *Config) GetInt64(key string) (int64, error) {
	val, ok := self.KVs[key]
	if !ok {
		return -1, fmt.Errorf("Key %s does not exist", key)
	}
	return strconv.ParseInt(val, 10, 64)
}

func (self *Config) GetBool(key string) (bool, error) {
	val, ok := self.KVs[key]
	if !ok {
		return false, fmt.Errorf("Key %s does not exist", key)
	}
	return strconv.ParseBool(val)
}

func (self *Config) GetString(key string) (string, error) {
	val, ok := self.KVs[key]
	if !ok {
		return "", fmt.Errorf("Key %s does not exist", key)
	}
	return val, nil
}

func (self *Config) GetFloat32(key string) (float32, error) {
	val, ok := self.KVs[key]
	if !ok {
		return 0, fmt.Errorf("Key %s does not exist", key)
	}
	s, err := strconv.ParseFloat(val, 32)
	return float32(s), err
}

func (self *Config) GetFloat64(key string) (float64, error) {
	val, ok := self.KVs[key]
	if !ok {
		return 0, fmt.Errorf("Key %s does not exist", key)
	}
	return strconv.ParseFloat(val, 64)
}
