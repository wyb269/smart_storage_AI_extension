/**
 * 零依赖的最小 XLSX 生成器（仅用于 Mock 模式）
 *
 * 为什么手写而不是引库
 * 项目 `package.json` 与 `node_modules` 里没有任何 Excel/ZIP 库
 * （`xlsx`/`exceljs`/`jszip` 全部不存在），而边界约束要求不新增依赖。
 *
 * 为什么不返回 CSV 或 HTML 冒充 xlsx
 * 前端把下载文件命名为 `.xlsx` 并按 OOXML 的 MIME 构造 Blob。
 * 用 CSV/HTML 冒充会让 Excel 打开时弹「文件格式与扩展名不符」警告，
 * 比直接报错更容易误导验收人员。
 *
 * 实现范围
 * XLSX 本质是一个 ZIP 包。这里只实现读取所需的最小集合：
 * - 压缩方式固定为 STORED（不压缩），因此只需自己算 CRC32，
 *   不需要引 deflate 库（Node 的 zlib 也能压，但 STORED 更短且无需校验解压路径）；
 * - 单元格用 `inlineStr` 内联字符串，从而省掉 `sharedStrings.xml`；
 * - 单 sheet。
 */

// 项目 eslint 规则 node/prefer-global/buffer 禁止使用全局 Buffer
import { Buffer } from 'node:buffer';

const CRC_TABLE = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n += 1) {
    let c = n;
    for (let k = 0; k < 8; k += 1) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c;
  }
  return table;
})();

function crc32(buf: Buffer): number {
  let c = -1;
  for (let i = 0; i < buf.length; i += 1) {
    c = (c >>> 8) ^ CRC_TABLE[(c ^ buf[i]) & 0xff];
  }
  return (c ^ -1) >>> 0;
}

/** ZIP 的 MS-DOS 时间/日期字段 */
function dosDateTime(now: Date): { time: number; date: number } {
  return {
    time: (now.getHours() << 11) | (now.getMinutes() << 5) | (Math.floor(now.getSeconds() / 2) & 0x1f),
    date: (((now.getFullYear() - 1980) & 0x7f) << 9) | ((now.getMonth() + 1) << 5) | now.getDate(),
  };
}

interface ZipEntry {
  name: string;
  data: Buffer;
}

/** 打包为 STORED（未压缩）ZIP */
function buildZip(entries: ZipEntry[]): Buffer {
  const { time, date } = dosDateTime(new Date());
  const locals: Buffer[] = [];
  const centrals: Buffer[] = [];
  let offset = 0;

  for (const entry of entries) {
    const nameBuf = Buffer.from(entry.name, 'utf8');
    const crc = crc32(entry.data);
    const size = entry.data.length;

    const local = Buffer.alloc(30 + nameBuf.length);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4); // version needed
    local.writeUInt16LE(0, 6); // flags（文件名全 ASCII，无需 UTF-8 标志位）
    local.writeUInt16LE(0, 8); // method = STORED
    local.writeUInt16LE(time, 10);
    local.writeUInt16LE(date, 12);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(size, 18);
    local.writeUInt32LE(size, 22);
    local.writeUInt16LE(nameBuf.length, 26);
    local.writeUInt16LE(0, 28);
    nameBuf.copy(local, 30);
    locals.push(local, entry.data);

    const central = Buffer.alloc(46 + nameBuf.length);
    central.writeUInt32LE(0x02014b50, 0);
    central.writeUInt16LE(20, 4); // version made by
    central.writeUInt16LE(20, 6); // version needed
    central.writeUInt16LE(0, 8);
    central.writeUInt16LE(0, 10);
    central.writeUInt16LE(time, 12);
    central.writeUInt16LE(date, 14);
    central.writeUInt32LE(crc, 16);
    central.writeUInt32LE(size, 20);
    central.writeUInt32LE(size, 24);
    central.writeUInt16LE(nameBuf.length, 28);
    central.writeUInt16LE(0, 30);
    central.writeUInt16LE(0, 32);
    central.writeUInt16LE(0, 34);
    central.writeUInt16LE(0, 36);
    central.writeUInt32LE(0, 38);
    central.writeUInt32LE(offset, 42);
    nameBuf.copy(central, 46);
    centrals.push(central);

    offset += local.length + size;
  }

  const centralBuf = Buffer.concat(centrals);
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(entries.length, 8);
  eocd.writeUInt16LE(entries.length, 10);
  eocd.writeUInt32LE(centralBuf.length, 12);
  eocd.writeUInt32LE(offset, 16);
  eocd.writeUInt16LE(0, 20);

  return Buffer.concat([Buffer.concat(locals), centralBuf, eocd]);
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

/** 0 -> A, 25 -> Z, 26 -> AA */
function columnName(index: number): string {
  let n = index + 1;
  let name = '';
  while (n > 0) {
    const rem = (n - 1) % 26;
    name = String.fromCharCode(65 + rem) + name;
    n = Math.floor((n - 1) / 26);
  }
  return name;
}

/**
 * 单元格文本里以这些字符开头的，必须强制成字符串，
 * 否则 Excel 打开时会把它当公式执行（CSV/公式注入）。
 * 真实后端的 POI 走 `setCellValue(String)` 存为文本单元格，
 * Mock 这边用 inlineStr 同样存为文本，两侧行为一致。
 */
function sanitizeCell(value: unknown): string {
  const text = value === null || value === undefined ? '' : String(value);
  if (/^[=+\-@\t\r]/.test(text)) return `'${text}`;
  return text;
}

function sheetXml(rows: unknown[][]): string {
  const lines: string[] = [
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
    '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
    '<sheetData>',
  ];
  rows.forEach((row, rowIndex) => {
    const cells = row
      .map((cell, colIndex) => {
        const ref = `${columnName(colIndex)}${rowIndex + 1}`;
        return `<c r="${ref}" t="inlineStr"><is><t xml:space="preserve">${escapeXml(sanitizeCell(cell))}</t></is></c>`;
      })
      .join('');
    lines.push(`<row r="${rowIndex + 1}">${cells}</row>`);
  });
  lines.push('</sheetData></worksheet>');
  return lines.join('');
}

/**
 * 生成一个单 sheet 的 xlsx。
 *
 * @param sheetName 工作表名
 * @param rows 第一行为表头
 */
export function buildXlsx(sheetName: string, rows: unknown[][]): Buffer {
  const utf8 = (s: string) => Buffer.from(s, 'utf8');

  const contentTypes =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
    '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
    '<Default Extension="xml" ContentType="application/xml"/>' +
    '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
    '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>' +
    '</Types>';

  const rootRels =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
    '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>' +
    '</Relationships>';

  const workbook =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" ' +
    'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
    `<sheets><sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/></sheets>` +
    '</workbook>';

  const workbookRels =
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
    '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>' +
    '</Relationships>';

  return buildZip([
    { name: '[Content_Types].xml', data: utf8(contentTypes) },
    { name: '_rels/.rels', data: utf8(rootRels) },
    { name: 'xl/workbook.xml', data: utf8(workbook) },
    { name: 'xl/_rels/workbook.xml.rels', data: utf8(workbookRels) },
    { name: 'xl/worksheets/sheet1.xml', data: utf8(sheetXml(rows)) },
  ]);
}
