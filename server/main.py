import json
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Depends
from sqlalchemy.orm import Session

from .database import engine, Base, get_db
from .api import endpoints
from .websocket_manager import manager
from . import crud, schemas

# --- App Initialization ---
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Auto-Regadera API",
    description="API para gestionar y monitorizar un sistema de riego automático con WebSockets.",
    version="1.1.0",
)

# --- API Routers ---
app.include_router(endpoints.router, prefix="/api/v1", tags=["HTTP Endpoints"])

# --- WebSockets ---
@app.websocket("/ws/esp32-ingest")
async def websocket_ingest_endpoint(websocket: WebSocket, db: Session = Depends(get_db)):
    """
    Endpoint de WebSocket para que el ESP32 envíe datos.
    Los datos recibidos se guardan en la BD y se retransmiten a los clientes.
    """
    await manager.connect(websocket, channel="esp32")
    try:
        while True:
            data = await websocket.receive_text()
            try:
                # 1. Parsear el JSON recibido del ESP32
                payload = json.loads(data)
                reading_data = schemas.SensorReadingCreate(
                    humidity=payload.get('humedad'),
                    light=payload.get('luz'),
                    pump_status=payload.get('bomba'),
                    mode=payload.get('modo'),
                    soil_type=payload.get('suelo')
                )
                
                # 2. Guardar en la base de datos
                db_reading = crud.create_sensor_reading(db=db, reading=reading_data)
                
                # 3. Retransmitir a los clientes de la UI
                await manager.broadcast_json(db_reading.dict(), channel="ui_clients")

            except json.JSONDecodeError:
                await manager.broadcast(f"Error: Mensaje no es un JSON válido: {data}", channel="esp32")
            except Exception as e:
                # Manejar errores de validación o de base de datos
                await manager.broadcast(f"Error procesando el mensaje: {e}", channel="esp32")

    except WebSocketDisconnect:
        manager.disconnect(websocket, channel="esp32")
        await manager.broadcast("Un ESP32 se ha desconectado.", channel="ui_clients")


@app.websocket("/ws/ui-feed")
async def websocket_ui_feed_endpoint(websocket: WebSocket):
    """
    Endpoint de WebSocket para que las aplicaciones cliente (móvil/web) se conecten
    y reciban actualizaciones de datos en tiempo real.
    """
    await manager.connect(websocket, channel="ui_clients")
    try:
        # Enviar el último estado conocido al cliente recién conectado
        db = next(get_db())
        latest_reading = crud.get_latest_sensor_reading(db)
        if latest_reading:
            await websocket.send_json(latest_reading.dict())
        
        # Mantener la conexión abierta para recibir actualizaciones
        while True:
            await websocket.receive_text() # Esperar cualquier mensaje (o simplemente mantener viva la conexión)

    except WebSocketDisconnect:
        manager.disconnect(websocket, channel="ui_clients")


# --- Root Endpoint ---
@app.get("/", tags=["Root"])
def read_root():
    return {"message": "Bienvenido a la API de Auto-Regadera"}
