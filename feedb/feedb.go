package main

import (
	"bytes"
	"compress/zlib"
	"encoding/binary"
	"encoding/json"
	"errors"
	"flag"
	"hash/crc32"
	"io/ioutil"
	"log"
	"net/http"
	"path"
	"strconv"
	"strings"
	"sync"
	"syscall"
)

var (
	dbroot    = flag.String("dbroot", ".", "data dir root")
	addr      = flag.String("addr", ":7167", "HTTP service address (e.g., ':7167') ")
	NOT_FOUND = errors.New("Not found")
	CORRUPTED = errors.New("Corrupted")
	ZERO_KEY  = errors.New("Key can not be zero")
)

const (
	DATA_FILE        = "data"
	DB_PREFEX        = "/d/"
	INDEX_FILE       = "index"
	MIN_INDEX_LENGtH = 1024 * 1024 * 16 //  16M
	MIN_DATA_LENGtH  = 1024 * 1024 * 64
	DATA_GROW_STEP   = 1024 * 1024 * 256
)

type Item struct {
	Id   uint64
	Data string
}

type Dict struct {
	mu    sync.Mutex
	Name  string
	data  []byte //  dataIdx is the first 8 byte
	index []byte
}

type Feedb struct {
	mu     sync.Mutex
	tables map[string]*Dict
}

func segmentPath(path string) (name string, ids string) {
	name = path
	idx := strings.Index(path, "/")
	if idx > 0 {
		name = path[:idx]
		ids = path[idx+1:]
	}
	return
}

func (db *Feedb) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if strings.HasPrefix(r.RequestURI, DB_PREFEX) {
		name, ids := segmentPath(r.URL.Path[3:])
		partiton, exits := db.tables[name]
		if !exits {
			http.Error(w, "404 db not found", http.StatusNotFound)
			return
		}
		r.ParseForm()
		switch r.Method {
		case "GET":
			partiton.GetAndResponse(ids, w, r)
			return
		case "POST":
			id, err := strconv.ParseUint(r.Form.Get("id"), 10, 64)
			if err != nil || id <= 0 {
				http.Error(w, "id parameter is mandatatory", 400)
				return
			}

			count, _ := strconv.ParseInt(r.Form.Get("len"), 10, 64)
			body, err := ioutil.ReadAll(r.Body)
			if err != nil || count <= 0 || count != int64(len(body)) {
				http.Error(w, "len is required or not match", 400)
				return
			}
			partiton.Set(int(id), body) // 64 bit computer, int is 64 bit
			w.WriteHeader(200)
			w.Write([]byte("OK"))
			return
		}
	}

	http.Error(w, "404 page not found", http.StatusNotFound)
}

func deflateData(data []byte) ([]byte, error) {
	reader, err := zlib.NewReader(bytes.NewReader(data))
	if err != nil {
		return nil, err
	}
	return ioutil.ReadAll(reader)
}

func (db *Dict) GetAndResponse(idstr string, w http.ResponseWriter, r *http.Request) {
	deflate := r.Form.Get("deflate") != ""

	if strings.Index(idstr, "-") != -1 {
		ids := strings.Split(idstr, "-")
		items := make([]Item, len(ids))
		for idx, i := range ids {
			id, _ := strconv.ParseUint(i, 10, 64)
			item := Item{Id: id}
			data, err := db.Get(id)
			if err == nil {
				if deflate {
					d, err := deflateData(data)
					if err == nil {
						item.Data = string(d)
					}
				} else {
					item.Data = string(data)
				}
			}
			items[idx] = item
		}
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		bytes, err := json.Marshal(items)
		if err != nil {
			w.Write([]byte(err.Error()))
		} else {
			w.Write(bytes)
		}
	} else {
		id, _ := strconv.ParseUint(idstr, 10, 64)
		data, err := db.Get(id)
		if err != nil {
			http.Error(w, err.Error(), 404)
			return
		}
		if r.Form.Get("text") != "" {
			w.Header().Set("Content-Type", "text/plain")
		} else {
			w.Header().Set("Content-Type", "application/octet-stream")
		}

		if deflate {
			w.Header().Set("Content-Encoding", "deflate")
		}
		//			w.WriteHeader(200)
		w.Write(data)
	}
}

func (db *Dict) Get(key uint64) (data []byte, err error) {
	if key <= 0 {
		return nil, ZERO_KEY
	}

	// TODO make sure no lock is needed
	if uint64(len(db.index)) < key*8 {
		return nil, NOT_FOUND
	}

	idx := binary.BigEndian.Uint64(db.index[key*8:])
	offset := idx >> 24
	length := idx & 0xffffff

	if offset == 0 || length == 0 {
		return nil, NOT_FOUND
	}

	r := db.data[offset : offset+length]
	if crc32.ChecksumIEEE(r) != binary.BigEndian.Uint32(db.data[offset-4:]) {
		log.Println("ERROR: data corrupted:", key, string(r))
		return nil, CORRUPTED
	}

	// log.Println(string(ioutil.ReadAll(flate.NewReader(bytes.NewReader(r)))))
	return r, nil
}

func (db *Dict) Set(key int, bytes []byte) error {
	if key <= 0 {
		return ZERO_KEY
	}
	db.mu.Lock()
	defer db.mu.Unlock()
	indexIdx := key * 8

	if indexIdx+8 > len(db.index) { // expand index
		capacity := 1
		for capacity < indexIdx+8 {
			capacity = capacity << 1
		}
		if err := syscall.Munmap(db.index); err != nil {
			log.Println("ERROR: ", err)
			return err
		}
		db.index = openMmap(path.Join(*dbroot, db.Name, INDEX_FILE), int64(capacity))
	}

	// offset if where actually data is written
	// 8 byte for Index
	offset := int64(binary.BigEndian.Uint64(db.data)) + 4 + 8
	oldLength := int64(len(db.data))
	if offset+int64(len(bytes)) >= oldLength { // expand data
		if err := syscall.Munmap(db.data); err != nil {
			log.Println("ERROR: ", err)
			return err
		}
		db.data = openMmap(path.Join(*dbroot, db.Name, DATA_FILE), oldLength+DATA_GROW_STEP)
	}

	// write data
	copy(db.data[offset:], bytes) // leave 8 byte for key | length, 4 byte for crc
	binary.BigEndian.PutUint32(db.data[offset-4:], crc32.ChecksumIEEE(bytes))

	// write data index to the first 8 byte
	binary.BigEndian.PutUint64(db.data, uint64(offset+int64(len(bytes)))-8)

	idx := uint64(offset)<<24 + uint64(len(bytes))
	binary.BigEndian.PutUint64(db.index[key*8:], idx)

	return nil
}

func openMmap(f string, minLength int64) []byte {
	// will zero the bit
	fd, err := syscall.Open(f, syscall.O_RDWR|syscall.O_CREAT, 0666)
	if err != nil {
		log.Fatal(err)
	}

	var stat syscall.Stat_t
	syscall.Fstat(fd, &stat)
	size := stat.Size

	if size < minLength {
		syscall.Ftruncate(fd, minLength)
		size = minLength
	}

	data, err := syscall.Mmap(fd, 0, int(size), syscall.PROT_WRITE, syscall.MAP_SHARED)
	if err != nil {
		log.Fatal(err)
	}
	log.Println("open", f, "size:", size)
	syscall.Close(fd)
	return data
}

func getDB(name string) *Dict {
	index := openMmap(path.Join(*dbroot, name, INDEX_FILE), MIN_INDEX_LENGtH)
	data := openMmap(path.Join(*dbroot, name, DATA_FILE), MIN_DATA_LENGtH)
	return &Dict{index: index, data: data, Name: name}
}

func readExitingFiles(dbroot string) (db *Feedb) {
	files, err := ioutil.ReadDir(dbroot)

	if err != nil {
		log.Fatal(err)
	}

	db = &Feedb{tables: make(map[string]*Dict)}

	for _, file := range files {
		if file.IsDir() {
			if !strings.HasPrefix(file.Name(), ".") {
				db.tables[file.Name()] = getDB(file.Name())
			}
		}
	}
	return db
}

func main() {
	flag.Parse()

	// st := []byte("sdfsdf")

	// a := Item{
	// 	Id:   1,
	// 	Data: []byte{'a'},
	// }

	// j, e := json.Marshal([]Item{a})
	// log.Println(e, string(j))

	// log.Println(100, 100>>0)
	//	log.SetFlags(log.Llongfile | log.LstdFlags)

	//	arr := []byte{1, 2 , 3, 4, 5, 6, 7, 8}
	//
	//	log.Println(arr)
	//	log.Println(*(*[]uint64)(unsafe.Pointer(&arr)))

	//	header := *(*reflect.SliceHeader)(unsafe.Pointer(&arr))

	//	log.Println(header.Len, header.Cap)
	//	header.Len/=8
	//	header.Cap/=8

	//	k := *(*[]uint64)(unsafe.Pointer(&header))
	//	log.Println(header.Len, header.Cap, k)

	//	k := []uint64((*uint64)(unsafe.Pointer(&arr[0])))

	//	log.Println(arr, *k)

	//	log.Println([]uint64(arr))

	db := readExitingFiles(*dbroot)
	log.Println("listen on", *addr)
	err := http.ListenAndServe(*addr, db)
	if err != nil {
		log.Fatal("http.ListenAndServe: ", err)
	}
}
