from pydantic import BaseModel
from datetime import datetime

# --- Sensor Reading Schemas ---
class SensorReadingBase(BaseModel):
    humidity: float
    light: float
    pump_status: bool
    mode: str
    soil_type: str

class SensorReadingCreate(SensorReadingBase):
    pass

class SensorReading(SensorReadingBase):
    id: int
    timestamp: datetime

    class Config:
        orm_mode = True

# --- Watering Event Schemas ---
class WateringEventBase(BaseModel):
    duration_seconds: int
    reason: str

class WateringEventCreate(WateringEventBase):
    pass

class WateringEvent(WateringEventBase):
    id: int
    start_time: datetime

    class Config:
        orm_mode = True

# --- ESP32 Config Schemas ---
class ESPConfig(BaseModel):
    soil_type: str
    light_threshold: int
    watering_duration: int
    watering_interval: int
