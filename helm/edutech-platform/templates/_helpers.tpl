{{/*
EduTech Platform Helm Helpers
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "edutech.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "edutech.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Chart label — used in selector labels
*/}}
{{- define "edutech.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to ALL resources
*/}}
{{- define "edutech.labels" -}}
helm.sh/chart: {{ include "edutech.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: edutech-platform
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}

{{/*
Selector labels for a named service component.
Usage: {{ include "edutech.selectorLabels" (dict "app" "auth-svc") }}
*/}}
{{- define "edutech.selectorLabels" -}}
app: {{ .app }}
app.kubernetes.io/name: {{ .app }}
{{- end }}

{{/*
Build the full image reference for a service.
Usage: {{ include "edutech.image" (dict "name" "auth-svc" "Values" .Values) }}
*/}}
{{- define "edutech.image" -}}
{{- $registry := .Values.global.registry -}}
{{- $prefix := .Values.global.imagePrefix -}}
{{- $tag := .Values.global.imageTag -}}
{{- if $registry -}}
{{- printf "%s/%s/%s:%s" $registry $prefix .name $tag -}}
{{- else -}}
{{- printf "%s/%s:%s" $prefix .name $tag -}}
{{- end -}}
{{- end }}

{{/*
Standard container security context (non-root, drop all capabilities)
*/}}
{{- define "edutech.containerSecurityContext" -}}
allowPrivilegeEscalation: false
readOnlyRootFilesystem: false
capabilities:
  drop:
    - ALL
{{- end }}

{{/*
Standard pod security context
*/}}
{{- define "edutech.podSecurityContext" -}}
runAsNonRoot: true
runAsUser: 1001
fsGroup: 1001
{{- end }}

{{/*
Standard imagePullSecrets block
*/}}
{{- define "edutech.imagePullSecrets" -}}
{{- if .Values.global.imagePullSecrets }}
imagePullSecrets:
{{- range .Values.global.imagePullSecrets }}
  - name: {{ .name }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Standard envFrom block (ConfigMap + Secret)
*/}}
{{- define "edutech.envFrom" -}}
envFrom:
  - configMapRef:
      name: edutech-common-config
  - secretRef:
      name: edutech-secrets
{{- end }}

{{/*
Standard readiness probe.
Usage: {{ include "edutech.readinessProbe" (dict "port" 8182) }}
*/}}
{{- define "edutech.readinessProbe" -}}
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: {{ .port }}
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
  timeoutSeconds: 5
{{- end }}

{{/*
Standard liveness probe.
Usage: {{ include "edutech.livenessProbe" (dict "port" 8182) }}
*/}}
{{- define "edutech.livenessProbe" -}}
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ .port }}
  initialDelaySeconds: 60
  periodSeconds: 30
  failureThreshold: 3
  timeoutSeconds: 5
{{- end }}

{{/*
Prometheus scrape annotations for a pod.
Usage: {{ include "edutech.prometheusAnnotations" (dict "port" "8182") }}
*/}}
{{- define "edutech.prometheusAnnotations" -}}
prometheus.io/scrape: "true"
prometheus.io/port: {{ .port | quote }}
prometheus.io/path: "/actuator/prometheus"
{{- end }}
