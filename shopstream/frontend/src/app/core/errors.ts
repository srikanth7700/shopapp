import { HttpErrorResponse } from '@angular/common/http';

/** Turns an HTTP error (RFC 7807 ProblemDetail from Spring) into a readable message. */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    const body = err.error;
    if (body && typeof body === 'object') {
      if (body.errors && typeof body.errors === 'object') {
        return Object.entries(body.errors)
          .map(([field, message]) => `${field}: ${message}`)
          .join(', ');
      }
      if (body.detail) {
        return body.detail;
      }
    }
    return `Request failed (${err.status})`;
  }
  return 'Something went wrong';
}
