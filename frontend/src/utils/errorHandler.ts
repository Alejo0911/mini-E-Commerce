import axios, { type AxiosError } from 'axios';
import type { AppAxiosError } from '../types/Error';

export function getFriendlyErrorMessage(err: unknown): string {
    if (axios.isAxiosError(err)) {
        const customError = err as AxiosError<AppAxiosError>;
        const responseData = customError.response?.data;

        if (typeof responseData === 'object' && responseData !== null) {
            const errorObj = responseData as Record<string, any>;
            if (errorObj.friendlyMessage) return errorObj.friendlyMessage;
            if (errorObj.message) return errorObj.message;
        }

        return customError.message || 'Error de comunicación con el servidor.';
    }
    
    if (err instanceof Error) {
        return err.message;
    }

    return 'Ocurrió un error inesperado.';
}