import tkinter as tk
from tkinter import ttk
import subprocess
import threading
import time

def click_keys():
    while running:
        try:
            # Fokusowanie na oknie
            subprocess.run(['xdotool', 'search', '--name', window_name, 'windowactivate'], check=True)
            time.sleep(0.5)  # Mała przerwa żeby okno się aktywowało

            # Wysyłanie skrótu klawiszowego
            if key_choice.get() == "Ctrl+Enter":
                subprocess.run(['xdotool', 'key', 'ctrl+Return'])
            elif key_choice.get() == "Alt+Enter":
                subprocess.run(['xdotool', 'key', 'alt+Return'])
            elif key_choice.get() == "Enter":
                subprocess.run(['xdotool', 'key', 'Return'])

            print("Kliknięto:", key_choice.get())
            time.sleep(120)  # 2 minuty
        except subprocess.CalledProcessError:
            print("Nie znaleziono okna:", window_name)

def start_clicking():
    global running, window_name
    running = True
    window_name = window_entry.get()
    threading.Thread(target=click_keys, daemon=True).start()

def stop_clicking():
    global running
    running = False

# GUI
root = tk.Tk()
root.title("Automatyczne kliknięcie klawiszy")

tk.Label(root, text="Nazwa okna aplikacji:").pack()
window_entry = tk.Entry(root)
window_entry.pack()

tk.Label(root, text="Wybierz kombinację klawiszy:").pack()
key_choice = ttk.Combobox(root, values=["Ctrl+Enter", "Alt+Enter", "Enter"])
key_choice.current(0)
key_choice.pack()

tk.Button(root, text="Start", command=start_clicking).pack(pady=5)
tk.Button(root, text="Stop", command=stop_clicking).pack(pady=5)

root.mainloop()
