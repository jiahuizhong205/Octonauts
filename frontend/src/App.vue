<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">CampusHub</div>
      <RouterLink to="/requirements">需求大厅</RouterLink>
      <RouterLink to="/profile">个人资料</RouterLink>
      <RouterLink to="/notifications">消息通知</RouterLink>
    </aside>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>
