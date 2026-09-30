import { HttpErrorResponse } from '@angular/common/http';

import { errorMessage } from './errors';

describe('errorMessage', () => {
  it('uses the ProblemDetail "detail" field', () => {
    const err = new HttpErrorResponse({ status: 409, error: { status: 409, detail: 'Email already exists' } });
    expect(errorMessage(err)).toBe('Email already exists');
  });

  it('lists validation errors', () => {
    const err = new HttpErrorResponse({ status: 400, error: { detail: 'Validation failed', errors: { price: 'must be greater than 0' } } });
    expect(errorMessage(err)).toBe('price: must be greater than 0');
  });

  it('explains when the server cannot be reached', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });
});
