from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from datetime import datetime, timedelta

from .. import crud, schemas
from ..database import get_db

router = APIRouter()

@router.post("/readings/", response_model=schemas.SensorReading, summary="Registrar nueva lectura de sensor")
def create_sensor_reading_endpoint(reading: schemas.SensorReadingCreate, db: Session = Depends(get_db)):
    """
    Endpoint para que el ESP32 o cualquier otro cliente publique una nueva
    lectura de los sensores. Esta información se almacenará en la base de datos.
    """
    return crud.create_sensor_reading(db=db, reading=reading)

@router.get("/readings/", response_model=List[schemas.SensorReading], summary="Obtener últimas lecturas")
def read_sensor_readings_endpoint(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    """
    Obtiene una lista de las lecturas más recientes de los sensores.
    Ideal para que la app móvil visualice el historial.
    """
    readings = crud.get_sensor_readings(db, skip=skip, limit=limit)
    return readings

@router.get("/readings/latest/", response_model=schemas.SensorReading, summary="Obtener la lectura más reciente")
def read_latest_sensor_reading_endpoint(db: Session = Depends(get_db)):
    """
    Devuelve el último estado reportado por los sensores.
    Útil para un vistazo rápido en la app.
    """
    db_reading = crud.get_latest_sensor_reading(db)
    if db_reading is None:
        raise HTTPException(status_code=404, detail="No hay lecturas disponibles")
    return db_reading

@router.post("/watering-events/", response_model=schemas.WateringEvent, summary="Registrar un evento de riego")
def create_watering_event_endpoint(event: schemas.WateringEventCreate, db: Session = Depends(get_db)):
    """
    Registra un evento de riego, ya sea manual o automático.
    """
    return crud.create_watering_event(db=db, event=event)

@router.get("/watering-events/", response_model=List[schemas.WateringEvent], summary="Obtener historial de riegos")
def read_watering_events_endpoint(skip: int = 0, limit: int = 50, db: Session = Depends(get_db)):
    """
    Devuelve una lista de los últimos eventos de riego.
    """
    events = crud.get_watering_events(db, skip=skip, limit=limit)
    return events

@router.get("/stats/last-24h", summary="Obtener estadísticas de las últimas 24 horas")
def get_stats_last_24h(db: Session = Depends(get_db)):
    """
    Calcula y devuelve estadísticas agregadas (promedios, min, max)
    para la humedad y la luz durante las últimas 24 horas.
    """
    start_date = datetime.now() - timedelta(days=1)
    end_date = datetime.now()
    
    readings = crud.get_readings_by_time_range(db, start_date=start_date, end_date=end_date)
    
    if not readings:
        return {"message": "No hay datos en las últimas 24 horas."}

    total_readings = len(readings)
    avg_humidity = sum(r.humidity for r in readings) / total_readings
    avg_light = sum(r.light for r in readings) / total_readings
    max_humidity = max(r.humidity for r in readings)
    min_humidity = min(r.humidity for r in readings)
    
    return {
        "time_range": {"start": start_date, "end": end_date},
        "total_readings": total_readings,
        "humidity": {"average": avg_humidity, "max": max_humidity, "min": min_humidity},
        "light": {"average": avg_light}
    }
