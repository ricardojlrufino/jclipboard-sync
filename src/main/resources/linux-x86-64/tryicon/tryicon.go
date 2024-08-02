package main

import "C"

import (
	"fmt"
	"sync"

	"github.com/getlantern/systray"
    "github.com/getlantern/systray/example/icon"
)

var count int
var title string
var mtx sync.Mutex

//export Log
func Log(msg string) int {
	mtx.Lock()
	defer mtx.Unlock()
    fmt.Print("GoLog:")
	fmt.Println(msg)
	count++
	return count
}

//export LogPtr
func LogPtr(msg *string) int {
	return Log(*msg);
}

//export Open
func Open() {
	systray.Run(onReady, onExit)
}

//export SetTitle
func SetTitle(msg string) {
    mtx.Lock()
	defer mtx.Unlock()
    title = msg;
}

func onReady() {
    systray.SetTitle(title)
	systray.SetIcon(icon.Data)
	systray.SetTooltip("Pretty awesome超级棒")
	mQuit := systray.AddMenuItem("Quit", "Quit the whole app")

	// Sets the icon of a menu item. Only available on Mac and Windows.
	mQuit.SetIcon(icon.Data)
}

func onExit() {
	// clean up here
}


func main() {}