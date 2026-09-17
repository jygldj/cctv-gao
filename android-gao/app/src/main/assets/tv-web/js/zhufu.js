// 小D老师黄历观影系统 - 电视/横屏手机通用版（吉神卡片内横向排版版）
const almanacStyles = `
/* 基础重置 */
* {
    margin: 0;
    padding: 0;
    -webkit-box-sizing: border-box;
    box-sizing: border-box;
    -webkit-text-size-adjust: 100%;
    -webkit-tap-highlight-color: transparent;
}
 
body {
    font-family: "Microsoft YaHei", Arial, sans-serif;
    background: #111111; 
    color: #edf2f4; 
    line-height: 1.4;
    padding: 8px;
    height: 100vh;
    overflow: hidden;
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-orient: vertical;
    -webkit-box-direction: normal;
        -ms-flex-direction: column;
            flex-direction: column;
}
 
/* 主容器 */
.almanac-container {
    height: 36vh;
    min-height: 200px; 
    max-height: 360px;
    background: linear-gradient(135deg, rgba(255, 215, 0, 0.05) 0%, rgba(17, 17, 17, 0.5) 100%); /* 微调加入了一点点金色的光晕感 */
    border-radius: 8px;
    padding: 8px 10px;
    margin: 2.5vh auto;
    width: 96%;
    max-width: 660px; 
    box-shadow: 0 2px 4px rgba(0,0,0,0.3);
    border: 1px solid #D4AF37; 
    overflow: hidden;
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-orient: vertical;
    -webkit-box-direction: normal;
        -ms-flex-direction: column;
            flex-direction: column;
}
 
/* 顶部标题栏 */
.header {
    text-align: center;
    color: #D4AF37; 
    font-size: 1rem;
    text-shadow: 1px 1px 1px rgba(0,0,0,0.3);
    line-height: 1.5;
    -ms-flex-negative: 0;
        flex-shrink: 0;
    padding: 2px 0;
    margin-bottom: 5px;
    border-bottom: 1px dashed rgba(212, 175, 55, 0.4); 
    white-space: nowrap; 
    overflow: hidden;
    text-overflow: ellipsis; 
}

.header .lunar-short {
    font-size: 0.85rem; 
    color: #fff;
    background: rgba(212, 175, 55, 0.15);
    display: inline-block;
    padding: 1px 6px;
    border-radius: 4px;
    margin: 0 10px;
    vertical-align: middle;
}

.header .time-extra {
    font-size: 0.85rem;
    color: #D4AF37;
    font-weight: bold;
    display: inline-block;
    padding: 1px 6px;
    background: rgba(212, 175, 55, 0.1);
    border: 1px solid rgba(212, 175, 55, 0.3);
    border-radius: 4px;
    vertical-align: middle;
    margin-left: 5px;
    letter-spacing: 1px;
    font-family: "Consolas", "Microsoft YaHei", monospace; 
}

/* 主体内容区 */
.main-layout {
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-flex: 1;
        -ms-flex-positive: 1;
            flex-grow: 1;
    overflow: hidden;
    gap: 8px;
}

/* 左侧信息栏 */
.info-panel {
    width: 40%;
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-orient: vertical;
    -webkit-box-direction: normal;
        -ms-flex-direction: column;
            flex-direction: column;
    gap: 8px;
}

/* 宜忌卡片 */
.yiji-card {
    background: rgba(212, 175, 55, 0.12);
    border: 1px solid rgba(212, 175, 55, 0.3);
    border-radius: 6px;
    padding: 6px 8px;
    font-size: 0.75rem;
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-orient: vertical;
    -webkit-box-direction: normal;
        -ms-flex-direction: column;
            flex-direction: column;
    gap: 3px;
    -ms-flex-negative: 0;
        flex-shrink: 0;
}
.yiji-card .yi { color: #4cd964; font-weight: bold; }
.yiji-card .ji { color: #ff4d4f; font-weight: bold; }

/* 【关键修改】吉神网格：保持原本的 2x2 容器 */
.god-grid {
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -ms-flex-wrap: wrap;
        flex-wrap: wrap;
    -webkit-box-flex: 1;
        -ms-flex-positive: 1;
            flex-grow: 1;
    gap: 4px;
}

/* 【关键修改】吉神卡片内部：改为横向一行排开（原本是上下堆叠） */
.god-item {
    width: calc(50% - 2px); /* 保持原本宽度，构成2x2 */
    background: rgba(212, 175, 55, 0.1);
    border-radius: 6px;
    border: 1px solid rgba(212, 175, 55, 0.3);
    
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-orient: horizontal; /* 横向排列 */
    -webkit-box-direction: normal;
        -ms-flex-direction: row;
            flex-direction: row;
    -webkit-box-align: center;
        -ms-flex-align: center;
            align-items: center;
    -webkit-box-pack: center;
        -ms-flex-pack: center;
            justify-content: center;
    
    gap: 4px; /* 文字之间的间距 */
    font-size: 0.8rem;
    padding: 2px 4px;
    
    height: 50%; /* 自动撑满高度，减少空白 */
}

/* 吉神内部的图标和文字不再强制堆叠，全部内联 */
.god-item .icon { font-size: 1rem; line-height: 1; }
.god-item .dir { font-size: 0.7rem; color: #ccc; margin-left: 2px; }

/* 右侧月历 */
.calendar-panel {
    width: 60%;
    background: rgba(30, 30, 30, 0.6);
    border-radius: 6px;
    padding: 4px;
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-orient: vertical;
    -webkit-box-direction: normal;
        -ms-flex-direction: column;
            flex-direction: column;
    overflow: hidden;
}

/* 星期栏 */
.week-header {
    display: -ms-grid;
    display: grid;
    -ms-grid-columns: repeat(7, 1fr);
    grid-template-columns: repeat(7, 1fr);
    text-align: center;
    font-size: 0.7rem;
    color: #D4AF37;
    margin-bottom: 2px;
    -ms-flex-negative: 0;
        flex-shrink: 0;
}

/* 日期网格 */
.days-grid {
    display: -ms-grid;
    display: grid;
    -ms-grid-columns: repeat(7, 1fr);
    grid-template-columns: repeat(7, 1fr);
    grid-auto-rows: 1fr;
    -webkit-box-flex: 1;
        -ms-flex-positive: 1;
            flex-grow: 1;
    gap: 2px;
}
.day-cell {
    display: -webkit-box;
    display: -ms-flexbox;
    display: flex;
    -webkit-box-align: center;
        -ms-flex-align: center;
            align-items: center;
    -webkit-box-pack: center;
        -ms-flex-pack: center;
            justify-content: center;
    font-size: 0.8rem;
    color: #e0e0e0;
    border-radius: 4px;
}
.day-cell.other-month { opacity: 0.3; }
.day-cell.weekend { color: #D4AF37; }
.day-cell.today { 
    background: #D4AF37; 
    color: #121212; 
    font-weight: bold; 
    box-shadow: 0 0 6px rgba(212,175,55,0.6); 
}

/* =============================================
   横屏手机适配 (宽度600-950px)
   ============================================= */
@media screen and (min-width: 600px) and (max-width: 950px) {
    .almanac-container {
        height: 40vh; 
        max-height: 400px; 
        padding: 4px 6px;
    }
    .header {
        font-size: 0.8rem;
        margin-bottom: 2px;
        padding: 1px 0;
    }
    .header .lunar-short, .header .time-extra {
        font-size: 0.7rem;
        margin: 0 3px;
        letter-spacing: 0.5px;
    }
    .info-panel { width: 36%; gap: 3px; }
    .calendar-panel { width: 64%; }
    
    .yiji-card { padding: 2px 4px; font-size: 0.6rem; gap: 1px; }
    
    /* 横屏时吉神内部文字再缩小一点 */
    .god-item { font-size: 0.65rem; padding: 1px 2px; gap: 2px; }
    .god-item .icon { font-size: 0.8rem; }
    .god-item .dir { font-size: 0.6rem; }
    
    .week-header div { font-size: 0.6rem; margin-bottom: 1px;}
    .day-cell { font-size: 0.7rem; }
}

/* =============================================
   标准大屏 / 电视适配 (960px以上)
   ============================================= */
@media screen and (min-width: 960px) {
    .almanac-container {
        width: 80%;
        max-width: 800px;
    }
    .header { font-size: 1.2rem; }
    .header .lunar-short, .header .time-extra { font-size: 1rem; }
    .yiji-card, .god-item, .day-cell { font-size: 0.95rem; }
    .god-item .icon { font-size: 1.2rem; }
}
`;

const AlmanacSystem = {
    GOD_POSITIONS: {
        '财神': { direction: '东南', icon: '💰' },
        '喜神': { direction: '西南', icon: '❤️' },
        '福神': { direction: '正北', icon: '🎁' },
        '贵神': { direction: '东北', icon: '👑' }
    },
    
    initAlmanacSystem: function() {
        this.injectStyles();  
        this.createDOM();  
        
        if (!window.Lunar) {
            console.error('❌ Lunar.js加载失败，请检查文件路径');
            document.getElementById('lunar-date').textContent = '农历库加载失败';
            return;
        }
        
        this.updateDisplay();  
        
        setInterval(() => this.updateDisplay(), 1000); 
    },
    
    injectStyles: function() {
        const style = document.createElement('style');
        style.textContent = almanacStyles;
        document.head.appendChild(style);
    },
    
    createDOM: function() {
        const container = document.createElement('div');
        container.className = 'almanac-container';
        container.innerHTML = `
            <div class="header">
                祝亲朋好友观影愉快
                <span class="lunar-short" id="lunar-date"></span>
                <span class="time-extra" id="time-extra"></span>
            </div>
            <div class="main-layout">
                <div class="info-panel">
                    <div class="yiji-card" id="yiji-info"></div>
                    <div class="god-grid" id="god-positions"></div>
                </div>
                <div class="calendar-panel">
                    <div class="week-header">
                        <div>日</div><div>一</div><div>二</div><div>三</div><div>四</div><div>五</div><div>六</div>
                    </div>
                    <div class="days-grid" id="calendar-grid"></div>
                </div>
            </div>
        `;
        document.body.appendChild(container);
    },
    
    updateDisplay: function() {
        const now = new Date();
        const lunar = Lunar.fromDate(now);
        
        document.getElementById('lunar-date').innerHTML = 
            '🏮 ' + lunar.getYearInGanZhi() + '年 ' + lunar.getMonthInChinese() + '月' + lunar.getDayInChinese();
        
        const timePeriod = this.getCurrentTimePeriod(now); 
        const ganZhiHour = lunar.getTimeInGanZhi(); 
        
        const pad = (num) => num.toString().padStart(2, '0');
        const clockTime = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
        
        const timeHTML = `${ganZhiHour} ${timePeriod} · ${clockTime}`;
        document.getElementById('time-extra').innerHTML = timeHTML;
        
        this.updateYiJi(lunar);
        this.updateGodPositions();  
        
        if(!this.currentDateStr || this.currentDateStr !== now.toDateString()) {
            this.currentDateStr = now.toDateString();
            this.renderCalendar(now);
        }
    },
    
    getCurrentTimePeriod: function(date) {
        const hours = date.getHours();  
        if(hours >= 23 || hours < 1) return '子时';
        if(hours < 3) return '丑时';
        if(hours < 5) return '寅时';
        if(hours < 7) return '卯时';
        if(hours < 9) return '辰时';
        if(hours < 11) return '巳时';
        if(hours < 13) return '午时';
        if(hours < 15) return '未时';
        if(hours < 17) return '申时';
        if(hours < 19) return '酉时';
        if(hours < 21) return '戌时';
        return '亥时';
    },
    
    updateYiJi: function(lunar) {
        const yi = lunar.getDayYi().slice(0, 3).join(' ');
        const ji = lunar.getDayJi().slice(0, 3).join(' ');
        
        document.getElementById('yiji-info').innerHTML = 
            `<div><span class="yi">宜</span> ${yi}</div>
             <div><span class="ji">忌</span> ${ji}</div>`;
    },
    
    updateGodPositions: function() {
        let html = '';
        for(const name in this.GOD_POSITIONS) {
            if(this.GOD_POSITIONS.hasOwnProperty(name)) {
                const info = this.GOD_POSITIONS[name];
                // 【关键修改】调整了 HTML 结构，让神名和方位并排显示
                html += `
                    <div class="god-item">
                        <span>${info.icon} ${name}</span>
                        <span class="dir">${info.direction}</span>
                    </div>
                `;
            }
        }
        document.getElementById('god-positions').innerHTML = html;
    },
    
    renderCalendar: function(date) {
        const year = date.getFullYear();
        const month = date.getMonth();
        const firstDay = new Date(year, month, 1).getDay(); 
        const daysInMonth = new Date(year, month + 1, 0).getDate(); 
        const lastMonthDays = new Date(year, month, 0).getDate(); 
        
        let gridHTML = '';
        
        for(let i = firstDay - 1; i >= 0; i--) {
            gridHTML += `<div class="day-cell other-month">${lastMonthDays - i}</div>`;
        }
        
        for(let d = 1; d <= daysInMonth; d++) {
            const isToday = (d === date.getDate());
            const dayOfWeek = new Date(year, month, d).getDay();
            const isWeekend = (dayOfWeek === 0 || dayOfWeek === 6);
            
            gridHTML += `<div class="day-cell ${isToday ? 'today' : ''} ${isWeekend && !isToday ? 'weekend' : ''}">${d}</div>`;
        }
        
        const totalCells = gridHTML.split('day-cell').length - 1;
        const remainingCells = (totalCells % 7 === 0) ? 0 : (7 - (totalCells % 7));
        for(let i = 1; i <= remainingCells; i++) {
            gridHTML += `<div class="day-cell other-month">${i}</div>`;
        }
        
        document.getElementById('calendar-grid').innerHTML = gridHTML;
    }
};

console.log('%c   输入 AlmanacSystem.getBlessing()   获取特别祝福', 'color: #D4AF37; font-weight: bold;');
AlmanacSystem.getBlessing = function() {
    const blessings = [
        "🎬 观影愉快，吉时已到！",
        "🍿 爆米花备好了，好电影在等您",
        "🌟 吉星高照，看片必爽"
    ];
    alert(blessings[Math.floor(Math.random()*blessings.length)]);
};

document.addEventListener('DOMContentLoaded', () => {
    AlmanacSystem.initAlmanacSystem(); 
});