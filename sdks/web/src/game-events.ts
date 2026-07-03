/**
 * Oddsmaker Game Events SDK Helper
 * 提供游戏事件的辅助函数
 */

/**
 * 游戏事件类型
 */
export const GameEventType = {
  // 关卡事件
  LEVEL_START: 'level_start',
  LEVEL_COMPLETE: 'level_complete',
  LEVEL_FAIL: 'level_fail',
  
  // 战斗事件
  BATTLE_START: 'battle_start',
  BATTLE_END: 'battle_end',
  
  // 任务事件
  QUEST_ACCEPT: 'quest_accept',
  QUEST_COMPLETE: 'quest_complete',
  
  // 成就事件
  ACHIEVEMENT_UNLOCK: 'achievement_unlock',
  
  // 物品事件
  ITEM_GRANT: 'item_grant',
  ITEM_CONSUME: 'item_consume',
  
  // 货币事件
  CURRENCY_SOURCE: 'currency_source',
  CURRENCY_SINK: 'currency_sink',
  
  // 广告事件
  AD_WATCH: 'ad_watch',
  AD_REWARD: 'ad_reward',
  
  // 社交事件
  SOCIAL_INVITE: 'social_invite',
  SOCIAL_ACCEPT: 'social_accept',
  
  // 错误事件
  ERROR_CRASH: 'error_crash',
  ERROR_EXCEPTION: 'error_exception',
  ERROR_NETWORK: 'error_network'
};

/**
 * 战斗模式
 */
export const BattleMode = {
  PVP: 'pvp',
  PVE: 'pve',
  RANKED: 'ranked',
  CASUAL: 'casual',
  CUSTOM: 'custom'
};

/**
 * 战斗结果
 */
export const BattleResult = {
  WIN: 'win',
  LOSE: 'lose',
  DRAW: 'draw',
  ABANDON: 'abandon'
};

/**
 * 任务类型
 */
export const QuestType = {
  MAIN: 'main',
  SIDE: 'side',
  DAILY: 'daily',
  WEEKLY: 'weekly',
  ACHIEVEMENT: 'achievement'
};

/**
 * 物品稀有度
 */
export const ItemRarity = {
  COMMON: 'common',
  UNCOMMON: 'uncommon',
  RARE: 'rare',
  EPIC: 'epic',
  LEGENDARY: 'legendary'
};

/**
 * 广告类型
 */
export const AdType = {
  REWARDED: 'rewarded',
  INTERSTITIAL: 'interstitial',
  BANNER: 'banner',
  NATIVE: 'native'
};

/**
 * 社交动作
 */
export const SocialAction = {
  INVITE: 'invite',
  ACCEPT: 'accept',
  REJECT: 'reject',
  BLOCK: 'block',
  UNBLOCK: 'unblock',
  FRIEND_REQUEST: 'friend_request',
  FRIEND_ACCEPT: 'friend_accept'
};

/**
 * 错误类型
 */
export const ErrorType = {
  CRASH: 'crash',
  EXCEPTION: 'exception',
  NETWORK: 'network',
  TIMEOUT: 'timeout',
  VALIDATION: 'validation'
};

/**
 * 关卡事件属性
 */
export interface LevelEventProps {
  level_id: string;
  level_name?: string;
  level_progress?: number;
  level_score?: number;
  level_time_ms?: number;
  level_attempts?: number;
  difficulty?: string;
  game_mode?: string;
  [key: string]: any;
}

/**
 * 战斗事件属性
 */
export interface BattleEventProps {
  battle_id?: string;
  battle_mode: string;
  battle_result?: string;
  battle_duration_ms?: number;
  battle_rank?: number;
  battle_kills?: number;
  battle_deaths?: number;
  battle_assists?: number;
  game_mode?: string;
  match_id?: string;
  [key: string]: any;
}

/**
 * 任务事件属性
 */
export interface QuestEventProps {
  quest_id: string;
  quest_name?: string;
  quest_type?: string;
  quest_progress?: number;
  quest_objective_current?: number;
  quest_objective_target?: number;
  [key: string]: any;
}

/**
 * 成就事件属性
 */
export interface AchievementEventProps {
  achievement_id: string;
  achievement_name?: string;
  achievement_category?: string;
  achievement_points?: number;
  [key: string]: any;
}

/**
 * 物品事件属性
 */
export interface ItemEventProps {
  item_id: string;
  item_type?: string;
  item_name?: string;
  item_rarity?: string;
  item_quantity?: number;
  item_level?: number;
  [key: string]: any;
}

/**
 * 货币事件属性
 */
export interface CurrencyEventProps {
  currency_type: string;
  currency_amount: number;
  currency_balance?: number;
  currency_source?: string;
  currency_sink?: string;
  [key: string]: any;
}

/**
 * 广告事件属性
 */
export interface AdEventProps {
  ad_type?: string;
  ad_placement_id?: string;
  ad_reward_type?: string;
  ad_reward_amount?: number;
  ad_duration_ms?: number;
  ad_completed?: boolean;
  ad_network?: string;
  ad_placement?: string;
  ad_format?: string;
  ad_impression_id?: string;
  [key: string]: any;
}

/**
 * 社交事件属性
 */
export interface SocialEventProps {
  social_action: string;
  social_target_user_id?: string;
  social_target_player_id?: string;
  [key: string]: any;
}

/**
 * 错误事件属性
 */
export interface ErrorEventProps {
  error_type: string;
  error_message?: string;
  error_stack_trace?: string;
  error_fatal?: boolean;
  [key: string]: any;
}

/**
 * 游戏事件辅助函数类
 */
export class GameEvents {
  private track: (name: string, props?: Record<string, any>) => string;
  
  constructor(track: (name: string, props?: Record<string, any>) => string) {
    this.track = track;
  }
  
  // 关卡事件
  
  /**
   * 记录关卡开始事件
   */
  levelStart(props: LevelEventProps): string {
    return this.track(GameEventType.LEVEL_START, {
      ...props,
      game_event_type: GameEventType.LEVEL_START
    });
  }
  
  /**
   * 记录关卡完成事件
   */
  levelComplete(props: LevelEventProps): string {
    return this.track(GameEventType.LEVEL_COMPLETE, {
      ...props,
      game_event_type: GameEventType.LEVEL_COMPLETE
    });
  }
  
  /**
   * 记录关卡失败事件
   */
  levelFail(props: LevelEventProps): string {
    return this.track(GameEventType.LEVEL_FAIL, {
      ...props,
      game_event_type: GameEventType.LEVEL_FAIL
    });
  }
  
  // 战斗事件
  
  /**
   * 记录战斗开始事件
   */
  battleStart(props: BattleEventProps): string {
    return this.track(GameEventType.BATTLE_START, {
      ...props,
      game_event_type: GameEventType.BATTLE_START
    });
  }
  
  /**
   * 记录战斗结束事件
   */
  battleEnd(props: BattleEventProps): string {
    return this.track(GameEventType.BATTLE_END, {
      ...props,
      game_event_type: GameEventType.BATTLE_END
    });
  }
  
  // 任务事件
  
  /**
   * 记录任务接受事件
   */
  questAccept(props: QuestEventProps): string {
    return this.track(GameEventType.QUEST_ACCEPT, {
      ...props,
      game_event_type: GameEventType.QUEST_ACCEPT
    });
  }
  
  /**
   * 记录任务完成事件
   */
  questComplete(props: QuestEventProps): string {
    return this.track(GameEventType.QUEST_COMPLETE, {
      ...props,
      game_event_type: GameEventType.QUEST_COMPLETE
    });
  }
  
  // 成就事件
  
  /**
   * 记录成就解锁事件
   */
  achievementUnlock(props: AchievementEventProps): string {
    return this.track(GameEventType.ACHIEVEMENT_UNLOCK, {
      ...props,
      game_event_type: GameEventType.ACHIEVEMENT_UNLOCK
    });
  }
  
  // 物品事件
  
  /**
   * 记录物品授予事件
   */
  itemGrant(props: ItemEventProps): string {
    return this.track(GameEventType.ITEM_GRANT, {
      ...props,
      game_event_type: GameEventType.ITEM_GRANT
    });
  }
  
  /**
   * 记录物品消耗事件
   */
  itemConsume(props: ItemEventProps): string {
    return this.track(GameEventType.ITEM_CONSUME, {
      ...props,
      game_event_type: GameEventType.ITEM_CONSUME
    });
  }
  
  // 货币事件
  
  /**
   * 记录货币来源事件
   */
  currencySource(props: CurrencyEventProps): string {
    return this.track(GameEventType.CURRENCY_SOURCE, {
      ...props,
      game_event_type: GameEventType.CURRENCY_SOURCE
    });
  }
  
  /**
   * 记录货币消耗事件
   */
  currencySink(props: CurrencyEventProps): string {
    return this.track(GameEventType.CURRENCY_SINK, {
      ...props,
      game_event_type: GameEventType.CURRENCY_SINK
    });
  }
  
  // 广告事件
  
  /**
   * 记录广告观看事件
   */
  adWatch(props: AdEventProps): string {
    return this.track(GameEventType.AD_WATCH, {
      ...props,
      game_event_type: GameEventType.AD_WATCH
    });
  }
  
  /**
   * 记录广告奖励事件
   */
  adReward(props: AdEventProps): string {
    return this.track(GameEventType.AD_REWARD, {
      ...props,
      game_event_type: GameEventType.AD_REWARD
    });
  }
  
  // 社交事件
  
  /**
   * 记录社交邀请事件
   */
  socialInvite(props: SocialEventProps): string {
    return this.track(GameEventType.SOCIAL_INVITE, {
      ...props,
      game_event_type: GameEventType.SOCIAL_INVITE
    });
  }
  
  /**
   * 记录社交接受事件
   */
  socialAccept(props: SocialEventProps): string {
    return this.track(GameEventType.SOCIAL_ACCEPT, {
      ...props,
      game_event_type: GameEventType.SOCIAL_ACCEPT
    });
  }
  
  // 错误事件
  
  /**
   * 记录崩溃事件
   */
  errorCrash(props: ErrorEventProps): string {
    return this.track(GameEventType.ERROR_CRASH, {
      ...props,
      game_event_type: GameEventType.ERROR_CRASH,
      error_fatal: true
    });
  }
  
  /**
   * 记录异常事件
   */
  errorException(props: ErrorEventProps): string {
    return this.track(GameEventType.ERROR_EXCEPTION, {
      ...props,
      game_event_type: GameEventType.ERROR_EXCEPTION
    });
  }
  
  /**
   * 记录网络错误事件
   */
  errorNetwork(props: ErrorEventProps): string {
    return this.track(GameEventType.ERROR_NETWORK, {
      ...props,
      game_event_type: GameEventType.ERROR_NETWORK
    });
  }
}

/**
 * 创建游戏事件辅助函数实例
 */
export function createGameEvents(track: (name: string, props?: Record<string, any>) => string): GameEvents {
  return new GameEvents(track);
}