
$css = @'

/* === PREMIUM INSTALLATION PANEL === */
.install-overlay {
  position: fixed;
  inset: 0;
  background: rgba(10, 17, 40, 0.6);
  backdrop-filter: blur(8px);
  z-index: 9100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  animation: overlayIn 0.2s ease-out;
}
.install-panel {
  width: 100%;
  max-width: 860px;
  max-height: 88vh;
  background: #ffffff;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 32px 80px rgba(0,0,0,0.18), 0 8px 24px rgba(0,0,0,0.08);
  animation: panelZoom 0.3s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.install-header {
  padding: 24px 28px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.install-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.install-icon-wrap {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 6px 20px rgba(16,185,129,0.4);
}
.install-title { margin: 0; font-size: 1.1rem; font-weight: 700; color: #f8fafc; }
.install-subtitle { margin: 4px 0 0; font-size: 0.82rem; color: #94a3b8; }
.install-subtitle strong { color: #34d399; }
.install-close-btn {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.1);
  color: #94a3b8; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; flex-shrink: 0;
}
.install-close-btn:hover { background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.3); color: #fca5a5; }
.install-os-banner {
  display: flex; gap: 8px; flex-wrap: wrap;
  padding: 14px 28px; background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.install-os-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 12px; background: #eff6ff; color: #3b82f6;
  border: 1px solid #bfdbfe; border-radius: 20px;
  font-size: 0.75rem; font-weight: 600;
}
.install-search-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 28px; background: white;
  border-bottom: 1px solid #e2e8f0; color: #94a3b8;
}
.install-search-bar input { flex: 1; border: none; outline: none; font-size: 0.9rem; color: #1e293b; background: transparent; font-family: inherit; }
.install-search-bar input::placeholder { color: #cbd5e1; }
.result-count { font-size: 0.75rem; font-weight: 700; color: #10b981; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 12px; padding: 3px 10px; white-space: nowrap; }
.install-body { flex: 1; overflow-y: auto; padding: 20px 28px; background: #f8fafc; }
.equipment-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 14px; }
.equipment-card {
  background: white; border: 2px solid #e2e8f0; border-radius: 16px; padding: 18px;
  cursor: pointer; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; align-items: flex-start; gap: 14px; position: relative;
}
.equipment-card:hover { border-color: #10b981; box-shadow: 0 4px 20px rgba(16,185,129,0.12); transform: translateY(-2px); }
.equipment-card--selected { border-color: #10b981 !important; background: linear-gradient(135deg, #f0fdf4, #ecfdf5) !important; box-shadow: 0 4px 24px rgba(16,185,129,0.2) !important; }
.eq-radio { width: 18px; height: 18px; border-radius: 50%; border: 2px solid #cbd5e1; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 2px; transition: border-color 0.2s; }
.eq-radio--checked { border-color: #10b981; background: #10b981; }
.eq-radio-dot { width: 8px; height: 8px; border-radius: 50%; background: white; }
.eq-device-icon { width: 44px; height: 44px; background: linear-gradient(135deg, #eff6ff, #dbeafe); border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #3b82f6; flex-shrink: 0; }
.equipment-card--selected .eq-device-icon { background: linear-gradient(135deg, #d1fae5, #a7f3d0); color: #059669; }
.eq-info { flex: 1; min-width: 0; }
.eq-name { font-size: 0.95rem; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.eq-brand { font-size: 0.78rem; color: #64748b; margin-top: 2px; }
.eq-serial { font-family: monospace; color: #94a3b8; font-size: 0.72rem; }
.eq-specs { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 8px; }
.eq-spec-chip { font-size: 0.7rem; padding: 2px 8px; background: #f1f5f9; color: #475569; border-radius: 6px; border: 1px solid #e2e8f0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }
.eq-spec-chip strong { color: #334155; }
.eq-check { position: absolute; top: 14px; right: 14px; width: 28px; height: 28px; background: #10b981; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; animation: popIn 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes popIn { from { transform: scale(0); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.install-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 24px; color: #94a3b8; text-align: center; }
.install-empty svg { color: #cbd5e1; margin-bottom: 16px; }
.install-empty h4 { margin: 0 0 8px; font-size: 1rem; font-weight: 700; color: #475569; }
.install-empty p { margin: 0; font-size: 0.85rem; max-width: 340px; line-height: 1.6; }
.install-footer { padding: 18px 28px; border-top: 1px solid #e2e8f0; background: white; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.install-selection-info { display: flex; align-items: center; gap: 8px; font-size: 0.85rem; font-weight: 600; color: #10b981; }
.install-footer-actions { display: flex; gap: 10px; }
.btn-install-confirm { display: inline-flex; align-items: center; gap: 8px; padding: 10px 22px; background: linear-gradient(135deg, #10b981, #059669); color: white; border: none; border-radius: 12px; font-size: 0.88rem; font-weight: 700; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 14px rgba(16,185,129,0.35); }
.btn-install-confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(16,185,129,0.45); }
.btn-install-confirm:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }
.spinner { display: inline-block; width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.4); border-top-color: white; border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.install-toast { position: fixed; bottom: 32px; right: 32px; z-index: 9999; display: flex; align-items: center; gap: 12px; padding: 14px 20px; border-radius: 14px; font-size: 0.88rem; font-weight: 600; max-width: 420px; box-shadow: 0 8px 32px rgba(0,0,0,0.15); animation: toastIn 0.35s cubic-bezier(0.34, 1.2, 0.64, 1); pointer-events: none; }
.install-toast--success { background: #0f172a; color: #34d399; border: 1px solid #065f46; }
.install-toast--error { background: #1c0a0a; color: #fca5a5; border: 1px solid #7f1d1d; }
@keyframes toastIn { from { transform: translateY(20px) scale(0.95); opacity: 0; } to { transform: translateY(0) scale(1); opacity: 1; } }
'@

$path = 'c:\Users\Asus\Desktop\Nouveau dossier\PFE\frontend\src\app\os-management\os-management.component.css'
Add-Content -Path $path -Value $css
Write-Host "CSS appended successfully."
