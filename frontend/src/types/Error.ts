import type { AxiosError } from "axios";

export interface ApiErrorResponse {
    timestamp?: string;
    status: number;
    error: string;
    message: string;
    path?: string;
}

export type AppAxiosError = AxiosError<ApiErrorResponse> & {
    friendlyMessage?: string;
};
