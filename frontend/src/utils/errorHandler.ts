import axios, { type AxiosError } from 'axios';
import type { ApiErrorResponse } from '../types/Error';

/**
 * Extrae un mensaje de error legible y descriptivo a partir de un error desconocido,
 * procesando respuestas estandarizadas del backend y errores de red.
 */
export function getFriendlyErrorMessage(err: unknown): string {
    if (axios.isAxiosError(err)) {
        const axiosError = err as AxiosError<ApiErrorResponse>;
        const responseData = axiosError.response?.data;

        if (responseData && typeof responseData === 'object') {
            if (responseData.message && typeof responseData.message === 'string') {
                return responseData.message;
            }
            if (responseData.error && typeof responseData.error === 'string') {
                return responseData.error;
            }
        }

        if (axiosError.code === 'ERR_NETWORK') {
            return 'A connection to the server could not be established. Verify that the backend is running in: ' + import.meta.env.VITE_API_URL;
        }

        if (axiosError.response?.status === 404) {
            return 'The requested resource was not found';
        }

        if (axiosError.response?.status === 409) {
            return 'State conflict in the operation. The stock may have changed, or the order may have expired.';
        }

        if (axiosError.response?.status === 401 || axiosError.response?.status === 403) {
            return 'Unauthorized access to the operation.';
        }

        return axiosError.message || 'Communication error with the server.';
    }

    if (err instanceof Error) {
        return err.message;
    }

    return 'An unexpected error occurred in the application.';
}
