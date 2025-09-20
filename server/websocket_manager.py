from fastapi import WebSocket
from typing import List, Dict

class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, List[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, channel: str = "default"):
        await websocket.accept()
        if channel not in self.active_connections:
            self.active_connections[channel] = []
        self.active_connections[channel].append(websocket)

    def disconnect(self, websocket: WebSocket, channel: str = "default"):
        if channel in self.active_connections:
            self.active_connections[channel].remove(websocket)

    async def broadcast(self, message: str, channel: str = "default"):
        if channel in self.active_connections:
            for connection in self.active_connections[channel]:
                await connection.send_text(message)

    async def broadcast_json(self, data: dict, channel: str = "default"):
        if channel in self.active_connections:
            for connection in self.active_connections[channel]:
                await connection.send_json(data)

# Crear una instancia global para ser usada en la aplicación
manager = ConnectionManager()
