# 云辰盾公共 location 配置（被 ycd.conf 两个 server 块 include）
# 放到 /etc/nginx/conf.d/ycd-common.inc

client_max_body_size 12m;   # 与后端 multipart 上限匹配

# ── 后端 API（含受保护文件 /api/files/preview）──
location /api/ {
    proxy_pass http://127.0.0.1:8081;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 60s;
}

# 安全：/uploads 不做任何代理/静态映射——敏感图片只能走 /api/files/preview 鉴权访问

# ── PC 管理后台（Vue 打包产物）──
location / {
    root /data/ycd/pc-admin;        # ★ dist 上传到这里
    try_files $uri $uri/ /index.html;   # history 路由回退
    index index.html;
}

# 静态资源缓存
location ~* \.(js|css|png|jpg|jpeg|gif|svg|woff2?)$ {
    root /data/ycd/pc-admin;
    expires 7d;
    add_header Cache-Control "public";
}
