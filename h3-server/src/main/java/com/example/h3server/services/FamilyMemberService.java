package com.example.h3server.services;

import com.example.h3server.exception.CustomException;
import com.example.h3server.models.Couple;
import com.example.h3server.models.FamilyMember;
import com.example.h3server.models.FamilyTree;
import com.example.h3server.models.User;
import com.example.h3server.repositories.FamilyMemberRepository;
import com.example.h3server.repositories.FamilyTreeRepository;
import com.example.h3server.repositories.UserRepository;
import com.example.h3server.utils.ModelValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final UserRepository userRepository;
    private final CoupleService coupleService;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository,
                               FamilyTreeRepository familyTreeRepository,
                               UserRepository userRepository,
                               CoupleService coupleService) {
        this.familyMemberRepository = familyMemberRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.userRepository = userRepository;
        this.coupleService = coupleService;
    }

    public List<FamilyMember> getAllMembers(Long treeId, String username) {
        final FamilyTree familyTree = getTreeOrThrowException(treeId);
        final User user = userRepository.findByUsername(username);

        checkIfUserHaveAccess(familyTree, user);

        return familyMemberRepository.findAllByFamilyTreeId(treeId);
    }

    public List<Couple> getAllCouples(Long treeId, String username) {
        final FamilyTree familyTree = getTreeOrThrowException(treeId);
        final User user = userRepository.findByUsername(username);

        checkIfUserHaveAccess(familyTree, user);

        return coupleService.getAllCouples(familyTree);
    }

    public FamilyMember updateMember(Long treeId, Long memberId, FamilyMember familyMember, String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (!familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }

        FamilyMember memberFromDb = null;
        try {
            memberFromDb = this.familyMemberRepository.findByIdAndFamilyTreeId(memberId, treeId);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        if (memberFromDb == null) {
            throw new CustomException("The family member doesn't exist", HttpStatus.NOT_FOUND);
        }

        familyMember.setId(memberFromDb.getId());

        ModelValidator.validate(familyMember);
        return familyMemberRepository.save(familyMember);
    }

    public FamilyMember addMember(Long treeId,
                                  FamilyMember familyMember,
                                  Long primaryParentId,
                                  Long partnerParentId,
                                  String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (!familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }

        // todo validate parent ids (primaryParentId > 0 && partnerParentId >= 0)

        familyMember.setId(null);

        ModelValidator.validate(familyMember);
        final FamilyMember savedMember = familyMemberRepository.save(familyMember);

        coupleService.addChild(familyTree, primaryParentId, partnerParentId, familyMember.getId());

        return savedMember;
    }

    @Transactional
    public FamilyMember addPartner(Long treeId, FamilyMember familyMember, Long primaryParentId, String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (!familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }


        familyMember.setId(null);

        ModelValidator.validate(familyMember);
        final FamilyMember savedMember = familyMemberRepository.save(familyMember);

        coupleService.addPartner(familyTree, primaryParentId, savedMember.getId());

        return savedMember;
    }

    public FamilyMember addMainMember(Long treeId, FamilyMember familyMember, String username) {
        FamilyTree familyTree = getTreeOrThrowException(treeId);

        if (!familyTree.getUser().getUsername().equals(username)) {
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        }

        familyMember.setId(null);

        ModelValidator.validate(familyMember);
        final FamilyMember savedMember = familyMemberRepository.save(familyMember);

        coupleService.addMainMember(familyTree, savedMember.getId());

        return savedMember;
    }

    public void deleteMember(Long treeId, Long memberId, String username) {
        // todo fix

//        FamilyTree familyTree = getTreeOrThrowException(treeId);
//
//        if (!familyTree.getUser().getUsername().equals(username)) {
//            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
//        }
//
//        FamilyMember memberFromDb = familyTree.getFamilyMember(memberId);
//        if (memberFromDb == null) {
//            throw new CustomException("The family member doesn't exist", HttpStatus.NOT_FOUND);
//        }
//
//        familyTree.getFamilyMembers()
//                .stream()
//                .filter(member -> member.getPrimaryParent() != null
//                        && member.getPrimaryParent().getId().equals(memberId))
//                .forEach(member -> member.setPrimaryParent(null));
//
//        familyTree.getFamilyMembers()
//                .stream()
//                .filter(member -> member.getSecondaryParent() != null
//                        && member.getSecondaryParent().getId().equals(memberId))
//                .forEach(member -> member.setSecondaryParent(null));
//
//        familyTreeRepository.save(familyTree);
//        familyMemberRepository.delete(memberFromDb);
    }

//    private FamilyMember findParent(FamilyMember parent, FamilyTree familyTree, Boolean isFather) {
//        if (parent == null) {
//            return null;
//        }
//
//        Long parentId = parent.getId();
//        if (parentId == null) {
//            return null;
//        }
//
//        String errorMessage = "Invalid " + (isFather ? "father" : "mother") + " id";
//
//        FamilyMember foundParent = familyTree.getFamilyMember(parentId);
//
//        if (foundParent == null) {
//            throw new CustomException(errorMessage, HttpStatus.NOT_FOUND);
//        }
//
//        return foundParent;
//    }

    private FamilyTree getTreeOrThrowException(Long treeId) {
        return familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new CustomException("The family tree doesn't exist", HttpStatus.NOT_FOUND));
    }

    private void checkIfUserHaveAccess(FamilyTree familyTree, User user) {
        if (familyTree.getIsPrivate()
                && !user.getUsername().equals(familyTree.getUser().getUsername())
                && !user.isAdmin()) {
            throw new CustomException("The family tree doesn't exist", HttpStatus.NOT_FOUND);
        }
    }
}
