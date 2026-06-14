import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PcComponent, BuildResponse, BuildRequest, BuildWithComponents, ComponentType, ComponentSpec, ComponentSpecs } from '../models/component.model';
import { COMPONENT_SPECIFICATIONS } from '../constants/component-specifications.const';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ComponentService {
  private baseUrl = `${environment.apiUrl}/api/admin/components`;
  private buildBaseUrl = `${environment.apiUrl}/api/build`;

  constructor(private http: HttpClient) { }

  getAllComponents(): Observable<PcComponent[]> {
    return this.http.get<PcComponent[]>(this.baseUrl);
  }

  getComponentById(id: number): Observable<PcComponent> {
    return this.http.get<PcComponent>(`${this.baseUrl}/${id}`);
  }

  createComponent(component: PcComponent): Observable<PcComponent> {
    return this.http.post<PcComponent>(this.baseUrl, component);
  }

  updateComponent(id: number, component: PcComponent): Observable<PcComponent> {
    return this.http.put<PcComponent>(`${this.baseUrl}/${id}`, component);
  }

  deleteComponent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getSpecsForType(type: ComponentType): ComponentSpec[] {
    return COMPONENT_SPECIFICATIONS[type] || [];
  }

  generateBuild(buildRequest: BuildRequest): Observable<BuildResponse> {
    return this.http.post<BuildResponse>(`${this.buildBaseUrl}/generate`, buildRequest);
  }

  createBuild(buildRequest: BuildRequest): Observable<BuildResponse> {
    return this.http.post<BuildResponse>(`${this.buildBaseUrl}`, buildRequest);
  }

  getUserBuilds(userId: number): Observable<BuildWithComponents[]> {
    return this.http.get<BuildWithComponents[]>(`${this.buildBaseUrl}/user/${userId}/builds`);
  }

  deleteBuild(userId: number, buildId: number): Observable<void> {
    return this.http.delete<void>(`${this.buildBaseUrl}/${buildId}/user/${userId}`);
  }
}