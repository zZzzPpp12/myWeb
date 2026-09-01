<script setup></script>

<template>
  <div class="about-page">
    <div class="about-card card">
      <div class="about-hero">
        <span class="about-mark">M</span>
        <div>
          <h1 class="about-title">码社区</h1>
          <p class="about-sub">一个深度问答与技术交流相结合的内容式互动社区</p>
        </div>
      </div>

      <p class="about-desc">
        码社区是一个综合性的内容互动平台，兼具专业性与互动性：支持 Markdown
        图文创作与代码高亮、问答式双向评价（赞同 / 反对）、话题关注、沸点短内容动态流、
        声望等级与排行榜、消息通知等完整的内容生态功能。
      </p>

      <h2 class="about-h2">核心功能</h2>
      <ul class="about-list">
        <li>文章创作：Markdown 编辑、代码块高亮、话题标签</li>
        <li>互动体系：赞同 / 反对、多级评论、收藏、关注</li>
        <li>沸点动态：短内容流、评论点赞、@提及通知</li>
        <li>话题系统：关注话题、话题页文章流</li>
        <li>成长体系：声望值、六级行为等级徽章、热文榜 / 创作者榜</li>
        <li>消息通知：未读状态实时提醒</li>
      </ul>

      <h2 class="about-h2">技术栈</h2>
      <div class="about-tags">
        <span class="about-tag">Vue 3</span>
        <span class="about-tag">Vite</span>
        <span class="about-tag">Pinia</span>
        <span class="about-tag">Vue Router</span>
        <span class="about-tag">Spring Boot 3</span>
        <span class="about-tag">JPA</span>
        <span class="about-tag">H2</span>
        <span class="about-tag">JWT</span>
      </div>

      <h2 class="about-h2">关于作者</h2>
      <div class="about-author">
        <p class="about-author-text">
          本项目由独立开发者设计与实现，从需求、前后端开发到部署全部独立完成，
          致力于打造一个纯粹、友好的技术内容社区。
        </p>
        <a
          class="about-github"
          href="https://github.com/zZzzPpp12"
          target="_blank"
          rel="noopener noreferrer"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true">
            <path d="M12 .5C5.65.5.5 5.65.5 12c0 5.08 3.29 9.39 7.86 10.91.58.11.79-.25.79-.55 0-.27-.01-1.17-.02-2.12-3.2.7-3.88-1.36-3.88-1.36-.52-1.33-1.28-1.68-1.28-1.68-1.04-.71.08-.7.08-.7 1.15.08 1.76 1.19 1.76 1.19 1.03 1.76 2.69 1.25 3.35.96.1-.75.4-1.25.72-1.54-2.55-.29-5.24-1.28-5.24-5.68 0-1.26.45-2.28 1.19-3.09-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.17 1.18a11 11 0 0 1 5.78 0c2.2-1.49 3.16-1.18 3.16-1.18.63 1.59.24 2.76.12 3.05.74.81 1.18 1.83 1.18 3.09 0 4.41-2.69 5.38-5.26 5.66.41.36.78 1.06.78 2.14 0 1.54-.01 2.79-.01 3.17 0 .31.21.67.8.55A11.51 11.51 0 0 0 23.5 12C23.5 5.65 18.35.5 12 .5z"/>
          </svg>
          GitHub：zZzzPpp12
        </a>
      </div>

      <p class="about-footer">感谢每一位来访的开发者，欢迎注册体验与交流。</p>
    </div>
  </div>
</template>

<style scoped>
.about-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.about-card {
  padding: 28px 32px;
}

.about-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.about-mark {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--primary);
  color: #fff;
  font-weight: 700;
  font-size: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.about-title {
  font-size: 26px;
  font-weight: 700;
}

.about-sub {
  margin-top: 4px;
  color: var(--text-3);
  font-size: 14px;
}

.about-desc {
  color: var(--text-2);
  line-height: 1.8;
  font-size: 15px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.about-h2 {
  margin: 22px 0 12px;
  font-size: 17px;
  font-weight: 700;
}

.about-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text-2);
  font-size: 14.5px;
}

.about-list li {
  padding-left: 18px;
  position: relative;
  line-height: 1.6;
}

.about-list li::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
}

.about-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.about-tag {
  padding: 5px 12px;
  border-radius: 15px;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 13px;
  font-weight: 500;
}

.about-author {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.about-author-text {
  color: var(--text-2);
  line-height: 1.8;
  font-size: 15px;
}

.about-github {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  border-radius: 8px;
  border: 1px solid var(--border);
  color: var(--text);
  font-size: 14px;
  width: fit-content;
  transition: all 0.15s;
}

.about-github:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.about-footer {
  margin-top: 28px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  color: var(--text-3);
  font-size: 13.5px;
  text-align: center;
}

@media (max-width: 768px) {
  .about-card {
    padding: 20px 18px;
  }
}
</style>
