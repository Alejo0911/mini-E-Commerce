<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue';

// ─── Props ────────────────────────────────────────────────────────────────────
const props = defineProps<{
  /** ISO-string de la fecha de expiración de la orden (order.expiresAt) */
  expiresAt: string;
}>();

// ─── Emits ────────────────────────────────────────────────────────────────────
const emit = defineEmits<{
  /** Se emite una sola vez cuando el contador llega a cero */
  (e: 'expired'): void;
}>();

// ─── Estado interno ───────────────────────────────────────────────────────────
const remainingSeconds = ref<number>(0);
let intervalId: ReturnType<typeof window.setInterval> | null = null;

// ─── Helpers ──────────────────────────────────────────────────────────────────
function calculateRemaining(): void {
  const diff = Math.floor(
    (new Date(props.expiresAt).getTime() - Date.now()) / 1000
  );

  if (diff <= 0) {
    remainingSeconds.value = 0;
    stopTimer();
    emit('expired');
  } else {
    remainingSeconds.value = diff;
  }
}

function startTimer(): void {
  stopTimer();
  calculateRemaining();
  if (remainingSeconds.value > 0) {
    intervalId = window.setInterval(calculateRemaining, 1000);
  }
}

function stopTimer(): void {
  if (intervalId !== null) {
    clearInterval(intervalId);
    intervalId = null;
  }
}

// ─── Reaccionar a cambios de expiresAt (cambio de orden activa) ───────────────
watch(
  () => props.expiresAt,
  (newVal) => {
    if (newVal) {
      startTimer();
    } else {
      stopTimer();
    }
  },
  { immediate: true }
);

onUnmounted(stopTimer);

// ─── Computed públicos ────────────────────────────────────────────────────────
const formattedTime = computed<string>(() => {
  const mins = Math.floor(remainingSeconds.value / 60);
  const secs = remainingSeconds.value % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
});

const isUrgent = computed<boolean>(() => remainingSeconds.value < 60 && remainingSeconds.value > 0);
</script>

<template>
  <div class="timer-section">
    <p class="timer-label">Tiempo restante para completar el pago:</p>
    <div class="timer-clock" :class="{ urgent: isUrgent }">
      {{ formattedTime }}
    </div>
    <p class="timer-warning">
      El stock está reservado temporalmente. Si el tiempo expira, la reserva se revertirá de forma atómica.
    </p>
  </div>
</template>

<style scoped>
.timer-section {
  background: #fff3cd;
  border: 1px solid #ffeeba;
  padding: 1.2rem;
  border-radius: 6px;
  text-align: center;
  margin: 1.2rem 0;
}
.timer-label {
  margin: 0;
  font-size: 0.9rem;
  color: #856404;
}
.timer-clock {
  font-size: 2.2rem;
  font-weight: bold;
  color: #d35400;
  margin: 0.4rem 0;
  font-family: 'Courier New', Courier, monospace;
}
.timer-clock.urgent {
  color: #c0392b;
  animation: pulse 1s infinite;
}
.timer-warning {
  font-size: 0.8rem;
  color: #856404;
  margin: 0;
}
@keyframes pulse {
  0%   { opacity: 1; }
  50%  { opacity: 0.4; }
  100% { opacity: 1; }
}
</style>

