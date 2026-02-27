{{/*
Expand the name of the chart.
*/}}
{{- define "ephor.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "ephor.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ephor.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ephor.labels" -}}
helm.sh/chart: {{ include "ephor.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
API labels
*/}}
{{- define "ephor.api.labels" -}}
{{ include "ephor.labels" . }}
app.kubernetes.io/name: {{ include "ephor.name" . }}-api
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: api
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
{{- end }}

{{/*
API selector labels
*/}}
{{- define "ephor.api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ephor.name" . }}-api
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Dashboard labels
*/}}
{{- define "ephor.dashboard.labels" -}}
{{ include "ephor.labels" . }}
app.kubernetes.io/name: {{ include "ephor.name" . }}-dashboard
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: dashboard
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
{{- end }}

{{/*
Dashboard selector labels
*/}}
{{- define "ephor.dashboard.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ephor.name" . }}-dashboard
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "ephor.postgresql.labels" -}}
{{ include "ephor.labels" . }}
app.kubernetes.io/name: {{ include "ephor.name" . }}-postgresql
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: postgresql
{{- end }}

{{/*
PostgreSQL selector labels
*/}}
{{- define "ephor.postgresql.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ephor.name" . }}-postgresql
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
API fullname
*/}}
{{- define "ephor.api.fullname" -}}
{{- printf "%s-api" (include "ephor.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Dashboard fullname
*/}}
{{- define "ephor.dashboard.fullname" -}}
{{- printf "%s-dashboard" (include "ephor.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
PostgreSQL fullname
*/}}
{{- define "ephor.postgresql.fullname" -}}
{{- printf "%s-postgresql" (include "ephor.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
API image
*/}}
{{- define "ephor.api.image" -}}
{{- printf "%s/%s:%s" .Values.api.image.registry .Values.api.image.repository (default .Chart.AppVersion .Values.api.image.tag) }}
{{- end }}

{{/*
Dashboard image
*/}}
{{- define "ephor.dashboard.image" -}}
{{- printf "%s/%s:%s" .Values.dashboard.image.registry .Values.dashboard.image.repository (default .Chart.AppVersion .Values.dashboard.image.tag) }}
{{- end }}

{{/*
Database host - returns internal service name or external host
*/}}
{{- define "ephor.database.host" -}}
{{- if .Values.postgresql.enabled }}
{{- include "ephor.postgresql.fullname" . }}
{{- else }}
{{- .Values.externalDatabase.host }}
{{- end }}
{{- end }}

{{/*
Database port
*/}}
{{- define "ephor.database.port" -}}
{{- if .Values.postgresql.enabled }}
{{- 5432 }}
{{- else }}
{{- .Values.externalDatabase.port }}
{{- end }}
{{- end }}

{{/*
Database name
*/}}
{{- define "ephor.database.name" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.auth.database }}
{{- else }}
{{- .Values.externalDatabase.database }}
{{- end }}
{{- end }}

{{/*
Database username
*/}}
{{- define "ephor.database.username" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.auth.username }}
{{- else }}
{{- .Values.externalDatabase.username }}
{{- end }}
{{- end }}

{{/*
Database JDBC URL
*/}}
{{- define "ephor.database.url" -}}
{{- printf "jdbc:postgresql://%s:%s/%s" (include "ephor.database.host" .) (include "ephor.database.port" . | toString) (include "ephor.database.name" .) }}
{{- end }}

{{/*
Secret name for database credentials
*/}}
{{- define "ephor.database.secretName" -}}
{{- if .Values.postgresql.enabled }}
  {{- if .Values.postgresql.auth.existingSecret }}
    {{- .Values.postgresql.auth.existingSecret }}
  {{- else }}
    {{- printf "%s-db" (include "ephor.fullname" .) }}
  {{- end }}
{{- else }}
  {{- if .Values.externalDatabase.existingSecret }}
    {{- .Values.externalDatabase.existingSecret }}
  {{- else }}
    {{- printf "%s-db" (include "ephor.fullname" .) }}
  {{- end }}
{{- end }}
{{- end }}

{{/*
Whether the chart should create the database secret
*/}}
{{- define "ephor.database.createSecret" -}}
{{- if .Values.postgresql.enabled }}
  {{- if not .Values.postgresql.auth.existingSecret }}true{{- end }}
{{- else }}
  {{- if not .Values.externalDatabase.existingSecret }}true{{- end }}
{{- end }}
{{- end }}

{{/*
Pod security context (OpenShift-compatible)
*/}}
{{- define "ephor.podSecurityContext" -}}
runAsNonRoot: true
seccompProfile:
  type: RuntimeDefault
{{- end }}

{{/*
Container security context (OpenShift-compatible)
*/}}
{{- define "ephor.containerSecurityContext" -}}
allowPrivilegeEscalation: false
capabilities:
  drop:
    - ALL
{{- end }}

{{/*
Image pull secrets
*/}}
{{- define "ephor.imagePullSecrets" -}}
{{- with .Values.global.imagePullSecrets }}
imagePullSecrets:
  {{- toYaml . | nindent 2 }}
{{- end }}
{{- end }}
