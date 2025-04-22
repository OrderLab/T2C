package bench

import (
	"fmt"
	"log"
	"path"
	"time"

	"github.com/go-zookeeper/zk"
)

type Client struct {
	Id        int
	Name      string
	Server    string
	Namespace string
	EndPoint  string
	Conn      *zk.Conn

	Stat     *BenchStat // the stats for requests issued by this client
	Children []*Client  // a client may have multiple child clients to launch concurrent requests
}

var (
	zkCreateFlags = int32(0)
	zkCreateACL   = zk.WorldACL(zk.PermAll)
)

type ConnLogger int32

func (l *ConnLogger) Printf(string, ...interface{}) {
	// do not print for now
}

func (self *Client) Log(spec string, args ...interface{}) {
	prefix := fmt.Sprintf("[Client %s->%s]: %s\n", self.Name, self.EndPoint, spec)
	log.Printf(prefix, args...)
}

func (self *Client) Read(rpath string) ([]byte, *zk.Stat, error) {
	if len(rpath) == 0 {
		return self.Conn.Get(self.Namespace)
	}
	return self.Conn.Get(self.Namespace + "/" + rpath)
}

func (self *Client) Write(rpath string, data []byte) error {
	var err error
	if len(rpath) == 0 {
		_, err = self.Conn.Set(self.Namespace, data, -1)
	} else {
		_, err = self.Conn.Set(self.Namespace+"/"+rpath, data, -1)
	}
	return err
}

func (self *Client) ReadWrite(rpath string, data []byte) error {
	if len(rpath) == 0 {
		rpath = self.Namespace
	} else {
		rpath = self.Namespace + "/" + rpath
	}
	_, stat, err := self.Conn.Get(rpath)
	if err != nil {
		return err
	}
	_, err = self.Conn.Set(rpath, data, stat.Version)
	return err
}

func (self *Client) Delete(rpath string) error {
	if len(rpath) == 0 {
		return self.Conn.Delete(self.Namespace, 0)
	}
	return self.Conn.Delete(self.Namespace+"/"+rpath, 0)
}

func (self *Client) DeleteR(rpath string) error {
	if len(rpath) == 0 {
		rpath = self.Namespace
	} else {
		rpath = self.Namespace + "/" + rpath
	}
	children, _, err := self.Conn.Children(rpath)
	if err != nil {
		return err
	}
	for _, child := range children {
		fpath := self.Namespace + "/" + child
		// log.Printf("Delete %s\n", fpath)
		err := self.Conn.Delete(fpath, -1)
		if err != nil {
			return err
		}
	}
	// log.Printf("Delete %s\n", rpath)
	return self.Conn.Delete(rpath, -1)
}

func (self *Client) Create(rpath string, data []byte) error {
	if len(rpath) == 0 {
		rpath = self.Namespace
	} else {
		rpath = self.Namespace + "/" + rpath
	}
	_, err := self.Conn.Create(rpath, data, zkCreateFlags, zkCreateACL)
	return err
}

func (self *Client) CreateR(rpath string, data []byte) error {
	if len(rpath) == 0 {
		rpath = self.Namespace
	} else {
		rpath = self.Namespace + "/" + rpath
	}
	var subps []string
	if len(rpath) > 0 && rpath != "/" {
		subps = append(subps, rpath)
	}
	for d := path.Dir(rpath); d != "." && d != "/"; {
		subps = append(subps, d)
		d = path.Dir(d)
	}
	l := len(subps) - 1
	var err error
	for i := range subps {
		subp := subps[l-i]
		if i != l {
			exists, _, err := self.Conn.Exists(subp)
			if err == nil && !exists {
				_, err = self.Conn.Create(subp, []byte(""), zkCreateFlags, zkCreateACL)
			}
		} else {
			_, err = self.Conn.Create(subp, data, zkCreateFlags, zkCreateACL)
		}
		if err != nil {
			return err
		}
	}
	return nil
}

func (self *Client) FullPath(rpath string) string {
	if len(rpath) == 0 {
		return self.Namespace
	}
	return self.Namespace + "/" + rpath
}

func (self *Client) CreateIfNotExist(rpath string, data []byte) (bool, error) {
	if len(rpath) == 0 {
		rpath = self.Namespace
	} else {
		rpath = self.Namespace + "/" + rpath
	}
	exists, _, err := self.Conn.Exists(rpath)
	if err != nil {
		return false, err
	}
	if !exists {
		_, err = self.Conn.Create(rpath, data, zkCreateFlags, zkCreateACL)
		return false, err
	}
	return true, nil
}

func (self *Client) Setup() error {
	exists, _, err := self.Conn.Exists(self.Namespace)
	if err != nil {
		return err
	}
	if !exists {
		err = self.CreateR("", []byte("I am client "+self.Name))
	}
	return err
}

func (self *Client) Cleanup() error {
	if self.Conn == nil {
		return nil
	}
	err := self.DeleteR("")
	self.Conn.Close()
	self.Conn = nil
	return err
}

func (self *Client) Reconnect() error {
	if self.Conn == nil {
		return nil
	}
	self.Conn.Close()
	self.Conn = nil
	conn, _, err := zk.Connect([]string{self.EndPoint}, 60*time.Minute)
	if err != nil {
		return err
	}
	var l ConnLogger
	conn.SetLogger(&l)
	self.Conn = conn
	return nil
}

func (self *Client) AddChildren(n int) error {
	if self.Children == nil {
		self.Children = make([]*Client, 0, n)
	}
	for i := 0; i < n; i++ {
		child, err := NewClient(self.Id, self.Name, self.Server, self.EndPoint, self.Namespace)
		if err != nil {
			self.Log("failed to create child client: %s", err)
		} else {
			self.Children = append(self.Children, child)
		}
	}
	return nil
}

func (self *Client) CloseChildren() {
	if self.Children == nil {
		// no child clients, great
		return
	}
	for _, child := range self.Children {
		child.Conn.Close()
		child.Conn = nil
	}
	self.Children = nil
}

func (self *Client) GetChild(i int) *Client {
	if self.Children == nil || i < 0 || i > len(self.Children) {
		return nil
	}
	return self.Children[i]
}

func retry(attempts int, sleep time.Duration, f func() error) (err error) {
    for i := 0; ; i++ {
        err = f()
        if err == nil {
            return
        }

        if i >= (attempts - 1) {
            break
        }

        time.Sleep(sleep)

        fmt.Println("retrying after error:", err)
    }
    return fmt.Errorf("after %d attempts, last error: %s", attempts, err)
}

func NewClient(id int, name string, server string, endpoint string, namespace string) (*Client, error) {
	conn, _, err := zk.Connect([]string{endpoint}, 60*time.Minute)
	if err != nil {
		return nil, err
	}
	var l ConnLogger
	conn.SetLogger(&l)
	return &Client{Id: id, Name: name, Server: server, Namespace: namespace, EndPoint: endpoint, Conn: conn}, nil
}

func NewClients(servers []string, endpoints []string, nclients int, namespace string) ([]*Client, error) {
	clients := make([]*Client, nclients)
	for i := 0; i < nclients; i++ {
		sid := fmt.Sprintf("%d", i+1)
		ns := namespace + "/client" + sid

		var client *Client
		err := retry(5, 10*time.Second, func() (err error) {
			client, err = NewClient(i+1, sid, servers[i%len(servers)], endpoints[i%len(endpoints)], ns)
			return
		})
		// client, err := NewClient(i+1, sid, servers[i%len(servers)], endpoints[i%len(endpoints)], ns)
		if err != nil {
			return nil, err
		}
		clients[i] = client
	}
	return clients, nil
}
