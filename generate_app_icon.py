#!/usr/bin/env python3
"""
앱 아이콘 생성 스크립트
파란색 그라데이션 배경에 흰색 강의실(문) 아이콘
"""

from PIL import Image, ImageDraw
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

def draw_room_icon(draw, center_x, center_y, icon_size, color=(255, 255, 255)):
    """강의실 문 아이콘 그리기"""
    # 문 테두리
    left = center_x - icon_size // 2
    top = center_y - icon_size // 2
    right = center_x + icon_size // 2
    bottom = center_y + icon_size // 2

    border_width = max(2, icon_size // 12)

    # 외부 사각형 (문 프레임)
    draw.rectangle([left, top, right, bottom], fill=color)

    # 내부 빈 공간 (문)
    inner_margin = border_width
    draw.rectangle(
        [left + inner_margin, top + inner_margin,
         right - inner_margin, bottom - inner_margin],
        fill=(0, 0, 0, 0)
    )

    # 문 손잡이
    handle_x = center_x + icon_size // 4
    handle_y = center_y
    handle_radius = max(2, icon_size // 16)
    draw.ellipse(
        [handle_x - handle_radius, handle_y - handle_radius,
         handle_x + handle_radius, handle_y + handle_radius],
        fill=color
    )

def create_icon_with_room(size, output_path, corner_radius_ratio=0.25):
    """강의실 아이콘이 포함된 완전한 앱 아이콘 생성"""

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

    # 강의실 아이콘 그리기
    draw = ImageDraw.Draw(output)
    icon_size = int(size * 0.45)  # 아이콘 크기는 전체의 45%
    draw_room_icon(draw, size // 2, size // 2, icon_size)

    # 저장
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    output.save(output_path, 'PNG')
    print(f"Created: {output_path}")

def create_foreground_only(size, output_path):
    """투명 배경에 강의실 아이콘만 있는 전경 이미지 생성 (adaptive icon용)"""

    # 투명 배경
    output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(output)

    # 강의실 아이콘 그리기 (adaptive icon은 안전 영역 고려)
    icon_size = int(size * 0.35)
    draw_room_icon(draw, size // 2, size // 2, icon_size)

    # 저장
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    output.save(output_path, 'PNG')
    print(f"Created: {output_path}")

def main():
    base_path = "app/src/main/res"

    # Adaptive icon foreground (투명 배경 + 강의실 아이콘만)
    print("Generating adaptive icon foregrounds (room icon only)...")
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
        create_icon_with_room(size, icon_path, corner_radius_ratio=0.22)

        # 둥근 아이콘
        round_path = f"{base_path}/mipmap-{density}/ic_launcher_round.png"
        create_icon_with_room(size, round_path, corner_radius_ratio=0.5)

    print("\n✓ All icons generated successfully!")
    print("\nAdaptive icon will use:")
    print("  - Background: Gradient from drawable/ic_launcher_background.xml")
    print("  - Foreground: PNG with white room/door icon")

if __name__ == "__main__":
    main()
