import json
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from sqlalchemy.orm import Session

from database import engine, Base, get_db
from api import endpoints
from websocket_manager import manager
import crud
import schemas

# --- App Initialization ---
print("🚀 Iniciando la aplicación FastAPI...")
Base.metadata.create_all(bind=engine)
print("✔️ Tablas de la base de datos verificadas/creadas.")

app = FastAPI(
    title="Auto-Regadera API",
    description="API para gestionar y monitorizar un sistema de riego automático con WebSockets.",
    version="1.1.0",
)

# --- API Routers ---
app.include_router(endpoints.router, prefix="/api/v1", tags=["HTTP Endpoints"])
print("✔️ Routers de la API HTTP incluidos.")

# --- WebSockets ---


@app.websocket("/ws/esp32-ingest")
async def websocket_ingest_endpoint(websocket: WebSocket):
    await manager.connect(websocket, channel="esp32")
    print("🔌 ESP32 conectado al WebSocket de ingesta.")
    db: Session = next(get_db())
    try:
        while True:
            data = await websocket.receive_text()
            print(f"📩 Datos recibidos del ESP32: {data}")
            try:
                payload = json.loads(data)
                reading_data = schemas.SensorReadingCreate(
                    humidity=payload.get('humedad'),
                    light=payload.get('luz'),
                    pump_status=payload.get('bomba'),
                    mode=payload.get('modo'),
                    soil_type=payload.get('suelo')
                )
                print("✅ Datos parseados y validados.")

                db_reading = crud.create_sensor_reading(
                    db=db, reading=reading_data)

                reading_dict = schemas.SensorReading.from_orm(
                    db_reading).dict()

                ui_clients_count = len(
                    manager.active_connections.get('ui_clients', []))
                if ui_clients_count > 0:
                    await manager.broadcast_json(reading_dict, channel="ui_clients")
                    print(
                        f"📡 Retransmitiendo a {ui_clients_count} clientes UI.")

            except json.JSONDecodeError:
                print(f"❌ JSON inválido recibido del ESP32: {data}")
            except Exception as e:
                print(f"❌ Error procesando mensaje del ESP32: {e}")

    except WebSocketDisconnect:
        manager.disconnect(websocket, channel="esp32")
        print("🔌 ESP32 desconectado.")
        await manager.broadcast_text("Un ESP32 se ha desconectado.", channel="ui_clients")
    finally:
        db.close()


@app.websocket("/ws/ui-feed")
async def websocket_ui_feed_endpoint(websocket: WebSocket):
    await manager.connect(websocket, channel="ui_clients")
    ui_clients_count = len(manager.active_connections.get('ui_clients', []))
    print(f"🖥️  Nuevo cliente UI conectado. Total: {ui_clients_count}")
    db: Session = next(get_db())
    try:
        latest_reading = crud.get_latest_sensor_reading(db)
        if latest_reading:
            reading_dict = schemas.SensorReading.from_orm(
                latest_reading).dict()
            await websocket.send_json(reading_dict)
            print(
                f"📈 Enviando último estado al nuevo cliente UI (ID: {latest_reading.id}).")

        while True:
            await websocket.receive_text()

    except WebSocketDisconnect:
        manager.disconnect(websocket, channel="ui_clients")
        print(
            f"🖥️  Cliente UI desconectado. Restantes: {len(manager.active_connections.get('ui_clients', []))}")
    finally:
        db.close()


# --- Root Endpoint ---
@app.get("/", tags=["Root"])
def read_root():
    print("🌐 Endpoint raíz '/' fue accedido.")
    return {"message": "Bienvenido a la API de Auto-Regadera"}


print("🎉 Aplicación lista para recibir conexiones.")
