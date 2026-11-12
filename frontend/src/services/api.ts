import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';

const api: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(
    (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
        const token = localStorage.getItem('accessToken');
        if(token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error: unknown) => Promise.reject(error)
);

api.interceptors.response.use(
    (response: AxiosResponse): AxiosResponse => response,
    (error: unknown) => {
        return Promise.reject(error);
    }
);

export default api;