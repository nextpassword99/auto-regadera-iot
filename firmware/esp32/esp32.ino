#include <WiFi.h>
#include <WebServer.h>

const char *ssid = "weeeeee";
const char *password = "123";

WebServer server(80);

const int PIN_SENSOR_HUMEDAD = 34;
const int PIN_SENSOR_LUZ = 35;
const int PIN_PUMP_IN1 = 26;
const int PIN_PUMP_IN2 = 27;
const int PIN_PUMP_EN = 25;

enum TipoSuelo
{
    ARENOSO,
    FRANCO,
    ARCILLOSO
};
TipoSuelo tipoSuelo = FRANCO;

const int UMBRAL_LUZ = 2000;
const int DURACION_RIEGO_MS = 10000;
const unsigned long INTERVALO_RIEGO_MS = 60000;

bool bombaEncendida = false;
bool modoManual = false;

unsigned long tiempoUltimoRiego = 0;

void setup()
{
    Serial.begin(115200);
    configurarPines();
    conectarWiFi();
    configurarServidorWeb();
}

void loop()
{
    server.handleClient();

    if (!modoManual && millis() - tiempoUltimoRiego >= INTERVALO_RIEGO_MS)
    {
        tiempoUltimoRiego = millis();
        manejarRiegoAutomatico();
    }
}

void configurarPines()
{
    pinMode(PIN_PUMP_IN1, OUTPUT);
    pinMode(PIN_PUMP_IN2, OUTPUT);
    pinMode(PIN_PUMP_EN, OUTPUT);
    apagarBomba();
}

void conectarWiFi()
{
    Serial.println("Conectando a WiFi...");
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\n✅ WiFi conectado");
    Serial.print("IP local: ");
    Serial.println(WiFi.localIP());
}

void configurarServidorWeb()
{
    server.on("/", HTTP_GET, manejarRoot);
    server.on("/status", HTTP_GET, manejarStatus);
    server.on("/start", HTTP_GET, manejarInicioManual);
    server.on("/stop", HTTP_GET, manejarDetenerManual);
    server.on("/setsoil", HTTP_GET, manejarCambiarTipoSuelo);
    server.begin();
    Serial.println("🌐 Servidor web iniciado");
}

int obtenerUmbralHumedad()
{
    switch (tipoSuelo)
    {
    case ARENOSO:
        return 350;
    case FRANCO:
        return 500;
    case ARCILLOSO:
        return 650;
    default:
        return 500;
    }
}

void manejarRiegoAutomatico()
{
    int humedad = analogRead(PIN_SENSOR_HUMEDAD);
    int luz = analogRead(PIN_SENSOR_LUZ);

    Serial.print("Humedad: ");
    Serial.print(humedad);
    Serial.print(" | Luz: ");
    Serial.print(luz);
    Serial.print(" (");
    Serial.print(luz >= UMBRAL_LUZ ? "claro" : "oscuro");
    Serial.println(")");

    if (humedad > obtenerUmbralHumedad() && luz >= UMBRAL_LUZ)
    {
        Serial.println("🚿 Riego activado automáticamente");
        encenderBomba();
        delay(DURACION_RIEGO_MS);
        apagarBomba();
    }
    else
    {
        Serial.println("✅ Condiciones adecuadas, no se riega");
    }
}

void encenderBomba()
{
    digitalWrite(PIN_PUMP_IN1, HIGH);
    digitalWrite(PIN_PUMP_IN2, LOW);
    digitalWrite(PIN_PUMP_EN, HIGH);
    bombaEncendida = true;
    Serial.println("💧 Bomba encendida");
}

void apagarBomba()
{
    digitalWrite(PIN_PUMP_EN, LOW);
    digitalWrite(PIN_PUMP_IN1, LOW);
    digitalWrite(PIN_PUMP_IN2, LOW);
    bombaEncendida = false;
    Serial.println("💧 Bomba apagada");
}

void manejarRoot()
{
    String html = "<h1>💧 Sistema de Riego Inteligente (ESP32)</h1>";
    html += "<p>Usa <code>/status</code>, <code>/start</code>, <code>/stop</code> o <code>/setsoil?tipo=franco</code></p>";
    server.send(200, "text/html", html);
}

void manejarStatus()
{
    int humedad = analogRead(PIN_SENSOR_HUMEDAD);
    int luz = analogRead(PIN_SENSOR_LUZ);

    String json = "{";
    json += "\"humedad\":" + String(humedad) + ",";
    json += "\"luz\":" + String(luz) + ",";
    json += "\"nivel_luz\":\"" + String(luz >= UMBRAL_LUZ ? "claro" : "oscuro") + "\",";
    json += "\"bomba\":\"" + String(bombaEncendida ? "encendida" : "apagada") + "\",";
    json += "\"modo\":\"" + String(modoManual ? "manual" : "automatico") + "\",";
    json += "\"suelo\":\"" + tipoSueloToString(tipoSuelo) + "\"";
    json += "}";

    server.send(200, "application/json", json);
}

void manejarInicioManual()
{
    modoManual = true;
    encenderBomba();
    server.send(200, "text/plain", "✅ Modo manual activado y bomba encendida");
}

void manejarDetenerManual()
{
    modoManual = false;
    apagarBomba();
    server.send(200, "text/plain", "⛔ Modo manual desactivado y bomba apagada");
}

void manejarCambiarTipoSuelo()
{
    if (!server.hasArg("tipo"))
    {
        server.send(400, "text/plain", "❌ Falta el parámetro 'tipo'");
        return;
    }

    String tipo = server.arg("tipo");

    if (tipo == "arenoso")
        tipoSuelo = ARENOSO;
    else if (tipo == "franco")
        tipoSuelo = FRANCO;
    else if (tipo == "arcilloso")
        tipoSuelo = ARCILLOSO;
    else
    {
        server.send(400, "text/plain", "❌ Tipo de suelo inválido. Usa: arenoso, franco o arcilloso");
        return;
    }

    server.send(200, "text/plain", "✅ Tipo de suelo cambiado a: " + tipo);
}

String tipoSueloToString(TipoSuelo tipo)
{
    switch (tipo)
    {
    case ARENOSO:
        return "arenoso";
    case FRANCO:
        return "franco";
    case ARCILLOSO:
        return "arcilloso";
    default:
        return "desconocido";
    }
}
