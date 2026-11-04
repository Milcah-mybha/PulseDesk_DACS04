#!/usr/bin/env python3
"""
PulseDesk TCP Socket Backend Server Test Simulator (Port 8888)
------------------------------------------------------------
Mục đích: Giả lập Backend Server phát các gói tin TCP Socket thời gian thực
tới ứng dụng Android PulseDesk qua cổng 8888.

Cách chạy:
    python3 socket_server.py

Khi ứng dụng Android kết nối vào, server sẽ tự động định kỳ gửi gói tin
thông báo tin nhắn mới để cập nhật huy hiệu (unread badge) phát sáng trên UI.
"""

import socket
import threading
import time
import json
import random

HOST = "0.0.0.0"
PORT = 8888

SAMPLE_MESSAGES = [
    {"convId": "conv_1", "text": "Em vừa gửi lại ảnh chụp màn hình mã lỗi qua email rồi ạ!"},
    {"convId": "conv_2", "text": "Kế toán bên em đã xác nhận thanh toán gói Enterprise thành công."},
    {"convId": "conv_4", "text": "Anh tư vấn thêm cho em về thời gian triển khai Chatbot AI với ạ."},
    {"convId": "conv_3", "text": "Đã hoàn tất cấu hình webhook, nhờ team test thử kết nối."},
]

def handle_client(client_socket, client_address):
    print(f"\n[+] Khách hàng Android kết nối từ: {client_address}")
    try:
        # Gửi lời chào kết nối ban đầu
        welcome_packet = {
            "type": "STATUS_CHANGE",
            "conversationId": "system",
            "message": "Kết nối máy chủ PulseDesk TCP Socket thành công!",
            "timestamp": "Bây giờ"
        }
        client_socket.sendall((json.dumps(welcome_packet) + "\n").encode("utf-8"))

        counter = 1
        while True:
            # Chờ 7 giây rồi gửi 1 tin nhắn thời gian thực để test badge tự động nhảy số
            time.sleep(7)
            sample = random.choice(SAMPLE_MESSAGES)
            packet = {
                "type": "NEW_MESSAGE",
                "conversationId": sample["convId"],
                "message": f"[{counter}] {sample['text']}",
                "timestamp": "Vừa xong",
                "unreadCount": random.randint(1, 6)
            }
            raw_data = json.dumps(packet) + "\n"
            client_socket.sendall(raw_data.encode("utf-8"))
            print(f"[->] Đã phát gói tin Socket tới Android: {packet}")
            counter += 1

    except (BrokenPipeError, ConnectionResetError):
        print(f"[-] Khách hàng {client_address} đã ngắt kết nối.")
    finally:
        client_socket.close()

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen(5)
    print(f"===========================================================")
    print(f"🚀 PulseDesk TCP Socket Server đang lắng nghe trên {HOST}:{PORT}")
    print(f"👉 Khởi chạy ứng dụng Android PulseDesk để bắt đầu nhận tin...")
    print(f"===========================================================")

    try:
        while True:
            client_sock, client_addr = server.accept()
            client_thread = threading.Thread(target=handle_client, args=(client_sock, client_addr), daemon=True)
            client_thread.start()
    except KeyboardInterrupt:
        print("\n[!] Dừng máy chủ Socket Server.")
    finally:
        server.close()

if __name__ == "__main__":
    main()
