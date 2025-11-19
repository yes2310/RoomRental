#!/usr/bin/env python3
"""
앱 아이콘 생성 스크립트
헤더 스타일과 동일한 파란색 그라데이션 배경에 흰색 "방" 텍스트
"""

from PIL import Image, ImageDraw, ImageFont
import os

def create_gradient(width, height, color1, color2):
    """대각선 그라데이션 생성"""
    base = Image.new('RGB', (width, height), color1)
    top = Image.new('RGB', (width, height), color2)
    mask = Image.new('L', (width, height))
    mask_data = []

    for y in range(height):
        for x in range(width):
            # 대각선 그라데이션 (135도)
            distance = (x + y) / (width + height)
            mask_data.append(int(255 * distance))

    mask.putdata(mask_data)
    base.paste(top, (0, 0), mask)
    return base

def create_icon_with_text(size, output_path, corner_radius_ratio=0.25):
    """텍스트가 포함된 완전한 아이콘 생성 (일반 launcher용)"""

    # 그라데이션 배경 생성 (#2196F3 -> #64B5F6)
    color1 = (33, 150, 243)   # #2196F3
    color2 = (100, 181, 246)  # #64B5F6

    img = create_gradient(size, size, color1, color2)

    # 둥근 모서리 적용
    corner_radius = int(size * corner_radius_ratio)

    # 마스크 생성 (둥근 모서리)
    mask = Image.new('L', (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle(
        [(0, 0), (size, size)],
        radius=corner_radius,
        fill=255
    )

    # 둥근 모서리 적용
    output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    output.paste(img, (0, 0))
    output.putalpha(mask)

    # 텍스트 그리기
    draw = ImageDraw.Draw(output)

    # 폰트 크기 계산 (아이콘 크기의 약 50%)
    font_size = int(size * 0.5)

    # 시스템 폰트 사용 (한글 지원 폰트)
    font = get_korean_font(font_size)

    # 텍스트 중앙 정렬
    text = "방"

    # 텍스트 크기 계산
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # 중앙 위치 계산
    x = (size - text_width) // 2 - bbox[0]
    y = (size - text_height) // 2 - bbox[1]

    # 흰색 텍스트 그리기
    draw.text((x, y), text, fill=(255, 255, 255, 255), font=font)

    # 저장
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    output.save(output_path, 'PNG')
    print(f"Created: {output_path}")

def create_foreground_only(size, output_path):
    """투명 배경에 텍스트만 있는 전경 이미지 생성 (adaptive icon용)"""

    # 투명 배경
    output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(output)

    # 폰트 크기 계산 (아이콘 크기의 약 40% - adaptive icon은 안전 영역 고려)
    font_size = int(size * 0.4)
    font = get_korean_font(font_size)

    # 텍스트 중앙 정렬
    text = "방"

    # 텍스트 크기 계산
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # 중앙 위치 계산
    x = (size - text_width) // 2 - bbox[0]
    y = (size - text_height) // 2 - bbox[1]

    # 흰색 텍스트 그리기
    draw.text((x, y), text, fill=(255, 255, 255, 255), font=font)

    # 저장
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    output.save(output_path, 'PNG')
    print(f"Created: {output_path}")

def get_korean_font(font_size):
    """한글 폰트 로드"""
    try:
        # 다양한 한글 폰트 경로 시도
        font_paths = [
            "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/System/Library/Fonts/AppleSDGothicNeo.ttc",
            "C:\\Windows\\Fonts\\malgun.ttf",  # Windows
        ]

        for font_path in font_paths:
            if os.path.exists(font_path):
                return ImageFont.truetype(font_path, font_size)

        # 폴백: 기본 폰트
        print(f"Warning: Using default font")
        return ImageFont.load_default()
    except:
        return ImageFont.load_default()

def main():
    base_path = "app/src/main/res"

    # Adaptive icon foreground (투명 배경 + 텍스트만)
    print("Generating adaptive icon foregrounds (text only)...")
    foreground_sizes = {
        'mdpi': 108,
        'hdpi': 162,
        'xhdpi': 216,
        'xxhdpi': 324,
        'xxxhdpi': 432
    }

    for density, size in foreground_sizes.items():
        fg_path = f"{base_path}/mipmap-{density}/ic_launcher_foreground.png"
        create_foreground_only(size, fg_path)

    # 각 해상도별 launcher icon 생성 (전체 아이콘)
    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }

    print("\nGenerating launcher icons (full) for all densities...")
    for density, size in sizes.items():
        # 일반 아이콘
        icon_path = f"{base_path}/mipmap-{density}/ic_launcher.png"
        create_icon_with_text(size, icon_path, corner_radius_ratio=0.22)

        # 둥근 아이콘
        round_path = f"{base_path}/mipmap-{density}/ic_launcher_round.png"
        create_icon_with_text(size, round_path, corner_radius_ratio=0.5)

    print("\n✓ All icons generated successfully!")
    print("\nAdaptive icon will use:")
    print("  - Background: Gradient from drawable/ic_launcher_background.xml")
    print("  - Foreground: PNG with white '방' text")

if __name__ == "__main__":
    main()
