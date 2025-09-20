from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import datetime

from .. import crud, schemas
from ..database import get_db

router = APIRouter()

@router.post("/readings/", response_model=schemas.SensorReading, summary="Registrar nueva lectura de sensor (HTTP)")
def create_sensor_reading_endpoint(reading: schemas.SensorReadingCreate, db: Session = Depends(get_db)):
    """
    Endpoint para que un cliente publique una nueva lectura de los sensores vía HTTP.
    Esta información se almacenará en la base de datos.
    """
    return crud.create_sensor_reading(db=db, reading=reading)

@router.get("/readings/", response_model=List[schemas.SensorReading], summary="Obtener lecturas con filtros")
def read_sensor_readings_endpoint(
    skip: int = 0, 
    limit: int = 100, 
    start_date: Optional[datetime] = Query(None, description="Fecha de inicio (formato ISO)"),
    end_date: Optional[datetime] = Query(None, description="Fecha de fin (formato ISO)"),
    db: Session = Depends(get_db)
):
    """
    Obtiene una lista de lecturas de sensores, con opción de paginación y filtrado por rango de fechas.
    """
    readings = crud.get_sensor_readings(db, skip=skip, limit=limit, start_date=start_date, end_date=end_date)
    return readings

@router.get("/readings/latest/", response_model=schemas.SensorReading, summary="Obtener la lectura más reciente")
def read_latest_sensor_reading_endpoint(db: Session = Depends(get_db)):
    """
    Devuelve el último estado reportado por los sensores.
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
def read_watering_events_endpoint(
    skip: int = 0, 
    limit: int = 50,
    start_date: Optional[datetime] = Query(None, description="Fecha de inicio (formato ISO)"),
    end_date: Optional[datetime] = Query(None, description="Fecha de fin (formato ISO)"),
    db: Session = Depends(get_db)
):
    """
    Devuelve una lista de los últimos eventos de riego, con opción de filtrar por fecha.
    """
    events = crud.get_watering_events(db, skip=skip, limit=limit, start_date=start_date, end_date=end_date)
    return events

@router.get("/stats/", summary="Obtener estadísticas en un rango de fechas")
def get_stats_in_range(
    start_date: datetime = Query(..., description="Fecha de inicio (formato ISO)"),
    end_date: datetime = Query(..., description="Fecha de fin (formato ISO)"),
    db: Session = Depends(get_db)
):
    """
    Calcula y devuelve estadísticas agregadas (promedios, min, max)
    para la humedad y la luz en un rango de fechas específico.
    """
    readings = crud.get_sensor_readings(db, limit=None, start_date=start_date, end_date=end_date) # No limit for stats
    
    if not readings:
        return {"message": f"No hay datos entre {start_date} y {end_date}."}

    total_readings = len(readings)
    avg_humidity = sum(r.humidity for r in readings) / total_readings
    avg_light = sum(r.light for r in readings) / total_readings
    max_humidity = max(r.humidity for r in readings)
    min_humidity = min(r.humidity for r in readings)
    
    return {
        "time_range": {"start": start_date, "end": end_date},
        "total_readings": total_readings,
        "humidity": {"average": f"{avg_humidity:.2f}", "max": max_humidity, "min": min_humidity},
        "light": {"average": f"{avg_light:.2f}"}
    }
