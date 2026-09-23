/** Absolute URL of an API path, as requested by the app (same origin as the page). */
export function apiUrl(path: string): string {
  return new URL(`/api/v1${path}`, window.location.origin).toString();
}
