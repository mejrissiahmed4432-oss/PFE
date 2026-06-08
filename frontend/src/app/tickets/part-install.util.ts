export interface InstalledPartPayload {
  name: string;
  qty: number;
  specification?: string;
  equipmentId?: string;
  replacesSpecKey?: string;
  actionType?: string;
  partType?: string;
  brand?: string;
}

export function buildPartSpecValue(part: InstalledPartPayload): string {
  const spec = (part.specification || '').trim();
  const name = (part.name || '').trim();
  const brand = (part.brand || '').trim();
  if (brand && spec) return `${brand} ${name} — ${spec}`.trim();
  if (spec) return `${name} — ${spec}`.trim();
  return name;
}

export function isReplaceAction(actionType?: string): boolean {
  const t = (actionType || '').toLowerCase();
  return /replace|upgrade|remove|swap/.test(t);
}

export function isInstallAction(actionType?: string): boolean {
  const t = (actionType || '').toLowerCase();
  return /install|add/.test(t) && !isReplaceAction(actionType);
}

export function extractGb(text: string): number | null {
  const match = (text || '').match(/(\d+)\s*gb/i);
  return match ? parseInt(match[1], 10) : null;
}

export function mergeSpecValues(current: string, addition: string, specKey: string): string {
  const key = (specKey || '').toLowerCase();
  const currentGb = extractGb(current);
  const addGb = extractGb(addition);

  if (currentGb != null && addGb != null) {
    const total = currentGb + addGb;
    if (key.includes('ram') || key.includes('memory') || /ram|memory|ddr/i.test(current + addition)) {
      return `${total} GB RAM`;
    }
    if (key.includes('storage') || key.includes('disk') || key.includes('ssd') || key.includes('hdd')) {
      return `${total} GB Storage`;
    }
    return `${total} GB`;
  }

  if (!current) return addition;
  return `${current} + ${addition}`;
}

export function applyPartToSpecifications(
  specifications: Record<string, string>,
  part: InstalledPartPayload
): { specKey: string; oldValue: string; newValue: string } | null {
  const specKey = (part.replacesSpecKey || part.partType || part.name || 'Part').trim();
  if (!specKey) return null;

  const current = (specifications[specKey] || '').trim();
  const partValue = buildPartSpecValue(part);
  let newValue: string;

  if (!current || isReplaceAction(part.actionType)) {
    newValue = partValue;
  } else if (isInstallAction(part.actionType)) {
    newValue = mergeSpecValues(current, partValue, specKey);
  } else {
    newValue = partValue;
  }

  specifications[specKey] = newValue;
  return { specKey, oldValue: current, newValue };
}
