package main

import (
	"encoding/binary"
	"errors"
	"flag"
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
)

const (
	DATA_FILE        = "data"
	DB_PREFEX        = "/d/"
	INDEX_FILE       = "index"
	MIN_INDEX_LENGtH = 1024 * 1024 * 16 //  16M
	MIN_DATA_LENGtH  = 1024 * 1024 * 64
	DATA_GROW_STEP   = 1024 * 1024 * 256
	META_LENGTH      = 12 // key(40bit), data length(24bit), crc(32 bit)
)

type Dict struct {
	mu    sync.Mutex
	Name  string
	data  []byte //  dataIdx is the first 8 byte
	index []byte //  indexIdx is the first 8 byte
}

type Feedb struct {
	mu     sync.Mutex
	tables map[string]*Dict
}

func (db *Feedb) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	uri := r.RequestURI
	if strings.HasPrefix(uri, DB_PREFEX) {
		partiton, exits := db.tables[r.URL.Path[3:]]
		if !exits {
			http.Error(w, "404 db not found", http.StatusNotFound)
			return
		}

		r.ParseForm()
		id, err := strconv.ParseUint(r.Form.Get("id"), 10, 64)

		if err != nil || id <= 0 {
			http.Error(w, "id parameter is mandatatory", 400)
			return
		}

		switch r.Method {
		case "GET":
			data, err := partiton.Get(id)
			if err != nil {
				http.Error(w, err.Error(), 404)
				return
			}
			if r.Form.Get("text") != "" {
				w.Header().Set("Content-Type", "text/plain")
			} else {
				w.Header().Set("Content-Type", "application/octet-stream")
			}

			if r.Form.Get("gzip") != "" {
				w.Header().Set("Content-Encoding", "gzip")
			}
			//			w.WriteHeader(200)
			w.Write(data)
			return
		case "POST":
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

func (db *Dict) Get(key uint64) (data []byte, err error) {
	// TODO make sure no lock is needed
	if uint64(len(db.index)) < key*8 {
		return nil, NOT_FOUND
	}
	// bigEndian  offset(40bit) | length(24bit)
	idx := db.index[key*8 : key*8+8]
	offset := uint64(idx[4]) | uint64(idx[3])<<8 | uint64(idx[2])<<16 |
		uint64(idx[1])<<24 | uint64(idx[0])<<32
	length := uint64(idx[7]) | uint64(idx[6])<<8 | uint64(idx[5])<<16

	if offset == 0 || length == 0 {
		return nil, NOT_FOUND
	}

	return db.data[offset : offset+length], nil
}

func (db *Dict) Set(key int, data []byte) error {
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

	offset := int64(binary.BigEndian.Uint64(db.data)) + META_LENGTH
	oldLength := int64(len(db.data))
	if offset+int64(len(data)) >= oldLength { // expand data
		if err := syscall.Munmap(db.data); err != nil {
			log.Println("ERROR: ", err)
			return err
		}
		db.data = openMmap(path.Join(*dbroot, db.Name, DATA_FILE), oldLength+DATA_GROW_STEP)
	}

	// write data
	copy(db.data[offset:], data) // leave 8 byte for key | length, 4 byte for crc
	binary.BigEndian.PutUint64(db.data, uint64(offset+int64(len(data))-META_LENGTH))

	// write index
	b := db.index
	start := key * 8
	b[start+0] = byte(offset >> 32)
	b[start+1] = byte(offset >> 24)
	b[start+2] = byte(offset >> 16)
	b[start+3] = byte(offset >> 8)
	b[start+4] = byte(offset)

	l := len(data)
	b[start+5] = byte(l >> 16)
	b[start+6] = byte(l >> 8)
	b[start+7] = byte(l)

	return nil
}

func openMmap(f string, minLength int64) []byte {
	// TODO close
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
