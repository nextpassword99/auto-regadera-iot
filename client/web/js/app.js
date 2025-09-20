document.addEventListener("DOMContentLoaded", () => {
  const WS_URL = "ws://127.0.0.1:8000/ws/ui-feed";
  const API_BASE_URL = "http://127.0.0.1:8000/api/v1";

  const elements = {
    connectionStatusText: document.getElementById("connection-status-text"),
    connectionStatusIndicator: document.getElementById(
      "connection-status-indicator"
    ),
    liveHumidity: document.getElementById("live-humidity"),
    liveLight: document.getElementById("live-light"),
    liveMode: document.getElementById("live-mode"),
    liveSoil: document.getElementById("live-soil"),
    livePumpIndicator: document.getElementById("live-pump-indicator"),
    livePumpStatus: document.getElementById("live-pump-status"),
    startDatePicker: document.getElementById("start-date-picker"),
    endDatePicker: document.getElementById("end-date-picker"),
    fetchStatsBtn: document.getElementById("fetch-stats-btn"),
    statsResults: document.getElementById("stats-results"),
    wateringHistoryBody: document.getElementById("watering-history-body"),
    sensorChartCanvas: document.getElementById("sensor-chart"),
  };

  let sensorChart;

  function connectWebSocket() {
    console.log("Intentando conectar al WebSocket...");
    const ws = new WebSocket(WS_URL);

    ws.onopen = () => {
      console.log("WebSocket conectado exitosamente.");
      updateConnectionStatus(true);
    };

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      console.log("Datos recibidos via WebSocket:", data);
      updateLiveCards(data);
      updateChart(data);
    };

    ws.onclose = () => {
      console.log(
        "WebSocket desconectado. Intentando reconectar en 3 segundos..."
      );
      updateConnectionStatus(false);
      setTimeout(connectWebSocket, 3000);
    };

    ws.onerror = (error) => {
      console.error("Error en WebSocket:", error);
      ws.close();
    };
  }

  function updateConnectionStatus(isConnected) {
    if (isConnected) {
      elements.connectionStatusText.textContent = "Conectado";
      elements.connectionStatusIndicator.classList.remove(
        "bg-red-500",
        "animate-pulse"
      );
      elements.connectionStatusIndicator.classList.add("bg-green-500");
    } else {
      elements.connectionStatusText.textContent = "Desconectado";
      elements.connectionStatusIndicator.classList.remove("bg-green-500");
      elements.connectionStatusIndicator.classList.add(
        "bg-red-500",
        "animate-pulse"
      );
    }
  }

  function updateLiveCards(data) {
    elements.liveHumidity.textContent = data.humidity.toFixed(1);
    elements.liveLight.textContent = data.light.toFixed(1);
    elements.liveMode.textContent = data.mode;
    elements.liveSoil.textContent = `Tipo de suelo: ${data.soil_type}`;
    if (data.pump_status) {
      elements.livePumpStatus.textContent = "Activa";
      elements.livePumpIndicator.classList.remove("bg-gray-600");
      elements.livePumpIndicator.classList.add("bg-blue-500", "animate-pulse");
    } else {
      elements.livePumpStatus.textContent = "Inactiva";
      elements.livePumpIndicator.classList.remove(
        "bg-blue-500",
        "animate-pulse"
      );
      elements.livePumpIndicator.classList.add("bg-gray-600");
    }
  }

  async function fetchWateringHistory() {
    try {
      const response = await fetch(`${API_BASE_URL}/watering-events/?limit=10`);
      if (!response.ok)
        throw new Error(`HTTP error! status: ${response.status}`);
      const events = await response.json();

      elements.wateringHistoryBody.innerHTML = "";
      events.forEach((event) => {
        const row = `
                    <tr class="bg-gray-800 border-b border-gray-700">
                        <td class="px-4 py-2">${new Date(
                          event.start_time
                        ).toLocaleString()}</td>
                        <td class="px-4 py-2">${event.duration_seconds}s</td>
                        <td class="px-4 py-2 capitalize">${event.reason}</td>
                    </tr>`;
        elements.wateringHistoryBody.innerHTML += row;
      });
    } catch (error) {
      console.error("Error al obtener historial de riego:", error);
    }
  }

  async function fetchStats(start, end) {
    if (!start || !end) {
      elements.statsResults.innerHTML = `<p class="col-span-2 text-amber-500">Por favor, selecciona un rango de fechas.</p>`;
      return;
    }
    try {
      const response = await fetch(
        `${API_BASE_URL}/stats/?start_date=${start.toISOString()}&end_date=${end.toISOString()}`
      );
      if (!response.ok)
        throw new Error(`HTTP error! status: ${response.status}`);
      const stats = await response.json();

      if (stats.message) {
        elements.statsResults.innerHTML = `<p class="col-span-2 text-gray-400">${stats.message}</p>`;
        return;
      }

      elements.statsResults.innerHTML = `
                <div class="bg-gray-700 p-3 rounded-lg">
                    <h3 class="text-sm font-semibold text-gray-400">Humedad Promedio</h3>
                    <p class="text-2xl font-bold text-cyan-400">${stats.humidity.average}</p>
                </div>
                <div class="bg-gray-700 p-3 rounded-lg">
                    <h3 class="text-sm font-semibold text-gray-400">Luz Promedio</h3>
                    <p class="text-2xl font-bold text-amber-400">${stats.light.average}</p>
                </div>
            `;
    } catch (error) {
      console.error("Error al obtener estadísticas:", error);
      elements.statsResults.innerHTML = `<p class="col-span-2 text-red-500">Error al cargar datos.</p>`;
    }
  }

  function initializeChart() {
    const ctx = elements.sensorChartCanvas.getContext("2d");
    sensorChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: [],
        datasets: [
          {
            label: "Humedad",
            data: [],
            borderColor: "rgb(56, 189, 248)",
            backgroundColor: "rgba(56, 189, 248, 0.1)",
            yAxisID: "y",
            tension: 0.3,
            fill: true,
          },
          {
            label: "Luz",
            data: [],
            borderColor: "rgb(251, 191, 36)",
            backgroundColor: "rgba(251, 191, 36, 0.1)",
            yAxisID: "y1",
            tension: 0.3,
            fill: true,
          },
        ],
      },
      options: {
        responsive: true,
        scales: {
          x: {
            ticks: { color: "#9ca3af" },
            grid: { color: "#374151" },
          },
          y: {
            type: "linear",
            display: true,
            position: "left",
            title: { display: true, text: "Humedad", color: "#67e8f9" },
            ticks: { color: "#9ca3af" },
            grid: { color: "#374151" },
          },
          y1: {
            type: "linear",
            display: true,
            position: "right",
            title: { display: true, text: "Luz", color: "#facc15" },
            ticks: { color: "#9ca3af" },
            grid: { drawOnChartArea: false },
          },
        },
        plugins: {
          legend: { labels: { color: "#d1d5db" } },
        },
      },
    });
  }

  async function fetchInitialChartData() {
    const endDate = new Date();
    const startDate = new Date(endDate.getTime() - 24 * 60 * 60 * 1000);
    const response = await fetch(
      `${API_BASE_URL}/readings/?start_date=${startDate.toISOString()}&end_date=${endDate.toISOString()}&limit=200`
    );
    const data = await response.json();
    data.reverse().forEach((reading) => updateChart(reading));
  }

  function updateChart(newData) {
    if (!sensorChart) return;

    const label = new Date(newData.timestamp).toLocaleTimeString();

    sensorChart.data.labels.push(label);
    sensorChart.data.datasets[0].data.push(newData.humidity);
    sensorChart.data.datasets[1].data.push(newData.light);

    const maxDataPoints = 50;
    if (sensorChart.data.labels.length > maxDataPoints) {
      sensorChart.data.labels.shift();
      sensorChart.data.datasets.forEach((dataset) => dataset.data.shift());
    }
    sensorChart.update();
  }

  function initializeDatePickers() {
    const commonOptions = {
      enableTime: true,
      dateFormat: "Y-m-d H:i",
      theme: "dark",
    };
    flatpickr(elements.startDatePicker, commonOptions);
    flatpickr(elements.endDatePicker, commonOptions);
  }

  function initializeEventListeners() {
    elements.fetchStatsBtn.addEventListener("click", () => {
      const start = elements.startDatePicker._flatpickr.selectedDates[0];
      const end = elements.endDatePicker._flatpickr.selectedDates[0];
      fetchStats(start, end);
    });
  }

  initializeChart();
  connectWebSocket();
  fetchWateringHistory();
  fetchInitialChartData();
  initializeDatePickers();
  initializeEventListeners();
});
