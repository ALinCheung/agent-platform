/**
 * Cron Builder - 可视化 Cron 表达式构建器
 * Alpine.js 组件，Dark OLED 科技风格
 */
function cronBuilder(initialValue) {
    return {
        // 当前选中的模板
        selectedTemplate: '',
        // 自定义配置
        minute: '*',
        hour: '*',
        dayOfMonth: '*',
        month: '*',
        dayOfWeek: '*',
        // 自定义输入模式
        customMinute: '',
        customHour: '',
        customDayOfMonth: '',
        customMonth: '',
        customDayOfWeek: '',
        // 每隔N分钟/小时模式
        everyNMinutes: 5,
        everyNHours: 1,
        minuteMode: 'all',    // all | every | specific
        hourMode: 'all',      // all | every | specific
        dayOfMonthMode: 'all', // all | specific
        monthMode: 'all',     // all | specific
        dayOfWeekMode: 'all', // all | specific

        // 预设模板
        templates: [
            { label: '每分钟', value: '* * * * *', cron: '* * * * *' },
            { label: '每5分钟', value: '*/5 * * * *', cron: '*/5 * * * *' },
            { label: '每15分钟', value: '*/15 * * * *', cron: '*/15 * * * *' },
            { label: '每30分钟', value: '*/30 * * * *', cron: '*/30 * * * *' },
            { label: '每小时', value: '0 * * * *', cron: '0 * * * *' },
            { label: '每天 00:00', value: '0 0 * * *', cron: '0 0 * * *' },
            { label: '每天 09:00', value: '0 9 * * *', cron: '0 9 * * *' },
            { label: '每天 18:00', value: '0 18 * * *', cron: '0 18 * * *' },
            { label: '工作日 09:00', value: '0 9 * * 1-5', cron: '0 9 * * 1-5' },
            { label: '每周一 09:00', value: '0 9 * * 1', cron: '0 9 * * 1' },
            { label: '每月1号 09:00', value: '0 9 1 * *', cron: '0 9 1 * *' },
            { label: '自定义', value: 'custom', cron: '' }
        ],

        // 星期映射
        weekDays: [
            { label: '周日', value: '0' },
            { label: '周一', value: '1' },
            { label: '周二', value: '2' },
            { label: '周三', value: '3' },
            { label: '周四', value: '4' },
            { label: '周五', value: '5' },
            { label: '周六', value: '6' }
        ],

        selectedWeekDays: [],

        init() {
            if (initialValue) {
                this.parseExisting(initialValue);
            }
        },

        // 选择模板
        selectTemplate(tpl) {
            this.selectedTemplate = tpl.value;
            if (tpl.value !== 'custom') {
                this.$dispatch('cron-change', tpl.cron);
            }
        },

        // 生成 cron 表达式
        get cronExpression() {
            if (this.selectedTemplate && this.selectedTemplate !== 'custom') {
                const tpl = this.templates.find(t => t.value === this.selectedTemplate);
                return tpl ? tpl.cron : '';
            }
            return this.buildCustomCron();
        },

        buildCustomCron() {
            const minute = this.minuteMode === 'every' ? '*/' + this.everyNMinutes :
                           this.minuteMode === 'specific' && this.customMinute ? this.customMinute : '*';
            const hour = this.hourMode === 'every' ? '*/' + this.everyNHours :
                         this.hourMode === 'specific' && this.customHour ? this.customHour : '*';
            const dom = this.dayOfMonthMode === 'specific' && this.customDayOfMonth ? this.customDayOfMonth : '*';
            const month = this.monthMode === 'specific' && this.customMonth ? this.customMonth : '*';
            const dow = this.dayOfWeekMode === 'specific' && this.selectedWeekDays.length > 0
                        ? this.selectedWeekDays.join(',') : '*';
            return `${minute} ${hour} ${dom} ${month} ${dow}`;
        },

        // 人类可读描述
        get humanReadable() {
            const cron = this.cronExpression;
            if (!cron) return '请配置执行时间';

            const parts = cron.split(/\s+/);
            if (parts.length !== 5) return '无效的表达式';

            const [min, hour, dom, month, dow] = parts;

            // 常见模式匹配
            if (cron === '* * * * *') return '每分钟执行';
            if (cron.match(/^\*\/(\d+) \* \* \* \*$/)) {
                return `每 ${RegExp.$1} 分钟执行`;
            }
            if (cron === '0 * * * *') return '每小时整点执行';
            if (cron.match(/^0 \*\/(\d+) \* \* \*$/)) {
                return `每 ${RegExp.$1} 小时执行`;
            }
            if (cron.match(/^(\d+) (\d+) \* \* \*$/)) {
                return `每天 ${hour.padStart(2, '0')}:${min.padStart(2, '0')} 执行`;
            }
            if (cron.match(/^(\d+) (\d+) \* \* ([\d,]+)$/)) {
                const dayNames = { '0': '周日', '1': '周一', '2': '周二', '3': '周三', '4': '周四', '5': '周五', '6': '周六' };
                const days = RegExp.$3.split(',').map(d => dayNames[d] || d).join('、');
                return `每${days} ${hour.padStart(2, '0')}:${min.padStart(2, '0')} 执行`;
            }
            if (cron.match(/^(\d+) (\d+) (\d+) \* \*$/)) {
                return `每月 ${dom} 号 ${hour.padStart(2, '0')}:${min.padStart(2, '0')} 执行`;
            }

            return cron;
        },

        // 解析已有的 cron 表达式
        parseExisting(expr) {
            if (!expr) return;
            const parts = expr.trim().split(/\s+/);
            if (parts.length !== 5) return;

            // 检查是否匹配模板
            const matched = this.templates.find(t => t.cron === expr);
            if (matched) {
                this.selectedTemplate = matched.value;
                return;
            }

            this.selectedTemplate = 'custom';
            const [min, hour, dom, month, dow] = parts;

            // 分钟
            if (min === '*') {
                this.minuteMode = 'all';
            } else if (min.startsWith('*/')) {
                this.minuteMode = 'every';
                this.everyNMinutes = parseInt(min.substring(2));
            } else {
                this.minuteMode = 'specific';
                this.customMinute = min;
            }

            // 小时
            if (hour === '*') {
                this.hourMode = 'all';
            } else if (hour.startsWith('*/')) {
                this.hourMode = 'every';
                this.everyNHours = parseInt(hour.substring(2));
            } else {
                this.hourMode = 'specific';
                this.customHour = hour;
            }

            // 日
            if (dom === '*') {
                this.dayOfMonthMode = 'all';
            } else {
                this.dayOfMonthMode = 'specific';
                this.customDayOfMonth = dom;
            }

            // 月
            if (month === '*') {
                this.monthMode = 'all';
            } else {
                this.monthMode = 'specific';
                this.customMonth = month;
            }

            // 星期
            if (dow === '*') {
                this.dayOfWeekMode = 'all';
            } else {
                this.dayOfWeekMode = 'specific';
                this.selectedWeekDays = dow.split(',');
            }
        },

        // 切换星期选择
        toggleWeekDay(day) {
            const idx = this.selectedWeekDays.indexOf(day);
            if (idx === -1) {
                this.selectedWeekDays.push(day);
                this.selectedWeekDays.sort();
            } else {
                this.selectedWeekDays.splice(idx, 1);
            }
        },

        isWeekDaySelected(day) {
            return this.selectedWeekDays.includes(day);
        }
    };
}
