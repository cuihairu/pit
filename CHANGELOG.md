# Changelog

## v0.2.0 (unreleased)
- Tracking Plan 字段字典规格化：属性定义新增 `cardinalityLimit` 上限（防高基数字段打爆存储）；ENUM 类型强制要求非空、无重复的 allowedValues JSON 数组且 cardinalityLimit ≥ 候选数；ARRAY 强制声明 arrayElementType
- 属性定义补齐 update/delete 接口（draft 计划内可编辑，软删除），增删改全量审计
- 新增 TrackingPlanServiceTest 10 个用例覆盖字段字典校验矩阵
- 环境策略绑定下发执行：Control 内部接口透出 `envStatus/envEnableSampling/envSampleRate`；Gateway 对非 active 环境返回 503 `environment_unavailable`，环境级确定性采样按 device_id SHA-256 分桶（同设备事件同进同出，保漏斗/留存口径），响应新增 `sampled_out` 计数
- 采样分桶弃用 String.hashCode（规整前缀聚集严重），改用 SHA-256 摘要取桶
- Game API 补齐默认时区：`games.default_timezone`（IANA 标识，默认 UTC），DTO 校验 + Service 层 ZoneId 白名单校验
- Game 生命周期操作接入审计日志：创建/更新/删除/发布/下线全量记录（P1「审计日志」覆盖 Game 资源）
- GameServiceTest 从空壳补齐 8 个用例：默认值、时区校验、状态机、软删除级联、审计断言
- API Key 角色化：keyType 统一为 `client/server/admin`；server key 强制 HMAC，admin key 只读；创建接口支持 `keyRole` 参数
- 客户端 SDK 移除 HMAC：iOS 删除 HMACManager，Unity 移除签名代码；HMAC 仅保留给 Server SDK
- Gateway HmacFilter 加固：client key 携带签名头返回 401 `signature_not_supported`，杜绝无 secret 验签 NPE；补充 client/server key 行为测试
- RiskRule API：新增 `/api/risk-rules` CRUD + enable/disable，Specification 多条件分页查询，操作全量写入审计日志

## v0.1.0 (initial release)
- Unified Java stack for ingest + streaming + analytics
- Gateway (Spring Boot): /v1/batch NDJSON+gzip; auth/HMAC; per-key & per-IP rate limit; JSON Schema; props allowlist; PII policy (mask/drop/coarse IP); DLQ; unified errors; OTel
- Control service: H2 persistence; API+Web UI; projects & API keys; dynamic policies (ratelimit/allowlist/PII); admin token
- Streaming (Flink): enrich (validate/dedup/UA/GeoIP), sessions, retention (D0/D1/D7/D30), funnels (two-step)
- Storage (ClickHouse): events/sessions; MVs & views (events/dau/revenue/ua/os)
- BI: Superset importable bundle; E2E script to import; dashboards for events, DAU, retention, funnel, revenue, UA/OS
- SDKs: Web (TS), Android (Kotlin), Unity (C#), iOS (Swift)
- Observability: OTel Collector → Prom & Grafana; docker-compose for local
- Dev/CI: k6 load test, perf matrix/report, E2E script, Gradle CI for gateway tests & Flink assemble & Web SDK build
- Deploy: Dockerfiles, Helm chart, raw K8s manifests, deployment docs
