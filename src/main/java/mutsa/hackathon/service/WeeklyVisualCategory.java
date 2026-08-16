package mutsa.hackathon.service;

public enum WeeklyVisualCategory {
    GRAPHIC_POSTER(ImageAspect.PORTRAIT),
    PHOTO_LANDSCAPE(ImageAspect.LANDSCAPE),
    NON_HUMAN_CHARACTER(ImageAspect.SQUARE),
    OIL_ACRYLIC(ImageAspect.SQUARE),
    ALBUM_COVER(ImageAspect.LANDSCAPE),
    PIXEL_ART(ImageAspect.SQUARE),
    FIRST_PERSON_ANIME(ImageAspect.PORTRAIT);

    private final ImageAspect imageAspect;

    WeeklyVisualCategory(ImageAspect imageAspect) {
        this.imageAspect = imageAspect;
    }

    public ImageAspect imageAspect() {
        return imageAspect;
    }

    public enum ImageAspect {
        SQUARE,
        PORTRAIT,
        LANDSCAPE
    }
}
