import os
import pypdfium2 as pdfium

src = r'f:\BaiduSyncdisk\04vscodeproj\manage\ai-1.pdf'
out_dir = r'f:\BaiduSyncdisk\04vscodeproj\manage\.figma_pages'
os.makedirs(out_dir, exist_ok=True)

pdf = pdfium.PdfDocument(src)
for i in range(len(pdf)):
    page = pdf[i]
    w, h = page.get_size()
    scale = min(1400 / w, 1400 / h, 1.0)
    img = page.render(scale=scale).to_pil()
    path = os.path.join(out_dir, f'page_{i+1}.png')
    img.save(path)
    print(i + 1, img.size)
