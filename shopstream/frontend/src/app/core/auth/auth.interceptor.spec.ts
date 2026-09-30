import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let token: string | null;
  const logout = jasmine.createSpy('logout');

  beforeEach(() => {
    token = null;
    logout.calls.reset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        // A fake AuthService: the test controls what token() returns.
        { provide: AuthService, useValue: { token: () => token, logout } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('adds the bearer token when logged in', () => {
    token = 'abc.def.ghi';

    http.get('/api/orders').subscribe();

    const req = httpMock.expectOne('/api/orders');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc.def.ghi');
    req.flush([]);
  });

  it('sends no Authorization header when logged out', () => {
    http.get('/api/products').subscribe();

    const req = httpMock.expectOne('/api/products');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('logs out when the gateway rejects the token', () => {
    token = 'expired.token.value';

    http.get('/api/orders').subscribe({ error: () => undefined });

    httpMock.expectOne('/api/orders').flush({ detail: 'Invalid or expired token' }, { status: 401, statusText: 'Unauthorized' });
    expect(logout).toHaveBeenCalled();
  });
});
