<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/views/common/header.jsp" />

<!-- Fonts -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<!-- Chart.js -->
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<style>
    .chart-container {
        position: relative;
        height: 300px;
        width: 100%;
    }
    .main-line-chart {
        height: 350px;
    }
    .trend-month-input {
        max-width: 170px;
    }
</style>

<jsp:include page="/WEB-INF/views/common/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4 mt-5">
                
                <!-- Page Header -->
                <div class="d-flex justify-content-between align-items-end mb-4">
                    <div>
                        <h2 class="h3 fw-bold mb-1">Reports & Analytics</h2>
                        <p class="text-muted mb-0">Platform overview and statistical trends</p>
                    </div>
                </div>

                <!-- Filters & Export Control -->
                <div class="auth-card p-4 mb-4">
                    <div class="row align-items-center mb-0">
                        <div class="col-md-4">
                            <div class="d-flex align-items-center">
                                <div class="icon-circle bg-primary bg-opacity-10 text-primary me-3" style="width: 48px; height: 48px;">
                                    <i class="bi bi-file-earmark-excel-fill fs-5"></i>
                                </div>
                                <div>
                                    <h6 class="fw-bold mb-1">Export Custom Report</h6>
                                    <p class="text-muted small mb-0">Select date range to download dataset</p>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-md-8">
                            <div class="row g-3 align-items-end">
                                <div class="col-md-4">
                                    <label class="form-label text-muted small fw-medium mb-1">From Date</label>
                                    <input type="date" id="fromDate" class="form-control bg-light border-0 shadow-none px-3" onchange="checkDates()">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label text-muted small fw-medium mb-1">To Date</label>
                                    <input type="date" id="toDate" class="form-control bg-light border-0 shadow-none px-3" onchange="checkDates()">
                                </div>
                                <div class="col-md-4">
                                    <button id="downloadBtn" class="btn btn-dark w-100 py-2 rounded-pill fw-medium shadow-sm d-flex align-items-center justify-content-center" disabled onclick="downloadReport()">
                                        <i class="bi bi-download me-2"></i>Download Excel
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Charts Grid -->
                <div class="row g-4 mb-4">
                    <!-- Pie Chart -->
                    <div class="col-lg-5">
                        <div class="auth-card p-4 h-100">
                            <div class="d-flex justify-content-between align-items-center mb-4">
                                <h6 class="text-muted text-uppercase tracking-wider small fw-bold mb-0">Complaint Status</h6>
                                <i class="bi bi-pie-chart text-muted"></i>
                            </div>
                            <div class="chart-container">
                                <canvas id="pieChart"></canvas>
                            </div>
                        </div>
                    </div>

                    <!-- Bar Chart -->
                    <div class="col-lg-7">
                        <div class="auth-card p-4 h-100">
                            <div class="d-flex justify-content-between align-items-center mb-4">
                                <h6 class="text-muted text-uppercase tracking-wider small fw-bold mb-0">Category Breakdown</h6>
                                <i class="bi bi-bar-chart text-muted"></i>
                            </div>
                            <div class="chart-container">
                                <canvas id="barChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Bottom Row Line Chart -->
                <div class="row">
                    <div class="col-12">
                        <div class="auth-card p-4">
                            <div class="d-flex justify-content-between align-items-center mb-4">
                                <h6 class="text-muted text-uppercase tracking-wider small fw-bold mb-0">Daily Complaint Submissions</h6>
                                <div class="d-flex align-items-center gap-2">
                                    <input
                                            type="month"
                                            id="trendMonth"
                                            class="form-control form-control-sm bg-light border-0 shadow-none px-3 trend-month-input"
                                            value="${currentYearMonth}"
                                            aria-label="Select month">
                                    <i class="bi bi-graph-up text-muted"></i>
                                </div>
                            </div>
                            <div class="chart-container main-line-chart">
                                <canvas id="lineChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>

            </div>

    <!-- Chart Configurations -->
    <script>
        // Use default font globally for ChartJS to match Inter
        Chart.defaults.font.family = "'Inter', sans-serif";
        Chart.defaults.color = '#6c757d'; // muted text

        // Safely parse JSON data from backend attributes
        let statusData = JSON.parse('${statusStatsJson}'.replace(/&quot;/g,'"'));
        let categoryData = JSON.parse('${categoryStatsJson}'.replace(/&quot;/g,'"'));
        let monthlyData = JSON.parse('${monthlyStatsJson}'.replace(/&quot;/g,'"'));
        
        // Define color palettes
        const modernColors = ['#ffc107', '#0d6efd', '#198754', '#dc3545', '#0dcaf0', '#adb5bd'];
        const softModernColors = modernColors.map(c => c + '80'); // Equivalent to bg-opacity-50
        
        // ------------------ PIE CHART ------------------ //
        new Chart(document.getElementById("pieChart"), {
            type: 'doughnut', // Doughnut looks more modern than standard pie
            data: {
                labels: Object.keys(statusData),
                datasets: [{
                    data: Object.values(statusData),
                    backgroundColor: ['#ffc107', '#0d6efd', '#198754', '#dc3545', '#6c757d'],
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '70%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            padding: 20
                        }
                    }
                }
            }
        });

        // ------------------ BAR CHART ------------------ //
        new Chart(document.getElementById("barChart"), {
            type: 'bar',
            data: {
                labels: Object.keys(categoryData),
                datasets: [{
                    label: "Complaints File",
                    data: Object.values(categoryData),
                    backgroundColor: '#0d6efd', 
                    borderRadius: 6, // rounded corners on bars
                    barPercentage: 0.6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { borderDash: [4, 4], color: '#e9ecef', drawBorder: false },
                        ticks: { stepSize: 1 }
                    },
                    x: {
                        grid: { display: false, drawBorder: false }
                    }
                }
            }
        });

        // ------------------ LINE CHART (MONTHLY) ------------------ //
        const lineChart = new Chart(document.getElementById("lineChart"), {
            type: 'line',
            data: {
                labels: Object.keys(monthlyData),
                datasets: [{
                    label: "Total Complaints",
                    data: Object.values(monthlyData),
                    borderColor: '#212529',
                    backgroundColor: 'rgba(33, 37, 41, 0.05)',
                    borderWidth: 2,
                    pointBackgroundColor: '#fff',
                    pointBorderColor: '#212529',
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    fill: true,
                    tension: 0.4 // Smooth curve
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#212529',
                        padding: 10,
                        cornerRadius: 8
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: { borderDash: [4, 4], color: '#e9ecef', drawBorder: false },
                        ticks: { stepSize: 5 }
                    },
                    x: {
                        grid: { display: false, drawBorder: false }
                    }
                }
            }
        });

        async function refreshLineChartForSelectedMonth() {
            const monthInput = document.getElementById("trendMonth");
            if (!monthInput || !monthInput.value) return;

            const parts = monthInput.value.split('-');
            if (parts.length !== 2) return;

            const year = parseInt(parts[0], 10);
            const month = parseInt(parts[1], 10);
            if (!Number.isFinite(year) || !Number.isFinite(month)) return;

            try {
                const response = await fetch('/admin/reports/daily-trends?year=' + encodeURIComponent(year) + '&month=' + encodeURIComponent(month));
                if (!response.ok) {
                    throw new Error(`Request failed: ${response.status}`);
                }

                const data = await response.json();
                const labels = Object.keys(data);
                const values = Object.values(data);

                lineChart.data.labels = labels;
                lineChart.data.datasets[0].data = values;
                lineChart.update();
            } catch (e) {
                console.error(e);
                alert("Unable to load selected month trend.");
            }
        }

        const trendMonthInput = document.getElementById("trendMonth");
        if (trendMonthInput) {
            trendMonthInput.addEventListener('change', refreshLineChartForSelectedMonth);
        }

        // ------------------ FILTER LOGIC ------------------ //
        function checkDates() {
            let from = document.getElementById("fromDate").value;
            let to = document.getElementById("toDate").value;
            let btn = document.getElementById("downloadBtn");
            btn.disabled = !(from && to);
        }

        function downloadReport() {
            let from = document.getElementById("fromDate").value;
            let to = document.getElementById("toDate").value;

            if (!from || !to) {
                alert("Please select both dates");
                return;
            }
            if (from > to) {
                alert("From date cannot be greater than To date");
                return;
            }
            window.location.href = "/admin/reports/download?from=" + from + "&to=" + to;
        }
    </script>

<jsp:include page="/WEB-INF/views/common/footer.jsp" />
