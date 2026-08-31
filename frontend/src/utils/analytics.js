import { boilingApi } from '@/api'

/**
 * 行为埋点上报：失败静默处理，不影响页面主流程
 * @param {string} action 动作名，如 boiling_view
 * @param {string} [targetType] 目标类型，如 BOILING / BOILING_COMMENT
 * @param {number|string} [targetId] 目标 id
 * @param {string} [extra] 附加信息
 */
export function track(action, targetType, targetId, extra) {
  return boilingApi
    .analytics({ action, targetType, targetId, extra })
    .catch(() => {})
}
