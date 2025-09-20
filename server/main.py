from fastapi import FastAPI
from .database import engine, Base
from .api import endpoints

# Crea las tablas en la base de datos (si no existen)
# En una aplicación de producción, probablemente usarías Alembic para migraciones.
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Auto-Regadera API",
    description="API para gestionar y monitorizar un sistema de riego automático.",
    version="1.0.0",
    contact={
        "name": "Tu Nombre",
        "email": "tu@email.com",
    },
    license_info={
        "name": "MIT",
    },
)

# Incluir los routers de la API
app.include_router(endpoints.router, prefix="/api/v1", tags=["Sensor Data & Events"])

@app.get("/", tags=["Root"])
def read_root():
    """
    Endpoint raíz que devuelve un mensaje de bienvenida.
    """
    return {"message": "Bienvenido a la API de Auto-Regadera"}

# Aquí podrías añadir más routers para diferentes partes de tu aplicación,
# por ejemplo, para la configuración del dispositivo.
